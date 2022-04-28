import argparse
import os
import socket
import struct
from enum import Enum
from socket import socket, IPPROTO_ICMP, gethostbyname, AF_INET, SOCK_RAW, gaierror, IPPROTO_IP, IP_TTL
from time import time, sleep
from typing import Optional

from select import select

ICMP_ECHO_REQUEST = 8
ICMP_ECHO_FORMAT = 'bbHHh'
ICMP_ECHO_HEADER_SIZE = 8

IP_HEADER_SIZE = 20

current_seq = 1


def calculate_checksum(data: bytes, reverse: bool = True) -> int:
    result = 0
    for i in range(0, len(data), 2):
        b1 = data[i]
        b2 = 0
        if i + 1 < len(data):
            b2 = data[i + 1]
        b = (b2 << 8) + b1
        result += b
    while (result & 0xffff) != result:
        result = (result >> 16) + (result & 0xffff)
    if reverse:
        result = result ^ 0xffff
    return result


def validate_checksum(data: bytes, checksum: int) -> bool:
    return (calculate_checksum(data, False) + checksum) == 0xffff


def build_echo_packet(packet_id: int, seq: int) -> bytes:
    header = struct.pack(ICMP_ECHO_FORMAT, ICMP_ECHO_REQUEST, 0, 0, packet_id, seq)
    data = round(time() * 1000).to_bytes(8, "big")
    checksum = calculate_checksum(header + data)
    header = struct.pack(ICMP_ECHO_FORMAT, ICMP_ECHO_REQUEST, 0, checksum, packet_id, seq)
    return header + data


def parse_icmp(raw_data: bytes) -> tuple[int, int, any]:
    icmp_part = raw_data[IP_HEADER_SIZE:]
    icmp_type, icmp_code = struct.unpack('bb', icmp_part[:2])
    if icmp_type == 0 and icmp_code == 0:
        icmp_header = icmp_part[:ICMP_ECHO_HEADER_SIZE]
        data = icmp_part[ICMP_ECHO_HEADER_SIZE:]
        icmp_type, icmp_code, checksum, p_id, s = struct.unpack(ICMP_ECHO_FORMAT, icmp_header)
        return icmp_type, icmp_code, (checksum, p_id, s, data)
    elif icmp_type == 3:
        return icmp_type, icmp_code, icmp_part[34:34 + 64]
    else:
        return icmp_type, icmp_code, icmp_part[2:]


class Error(Enum):
    TIMEOUT = -1
    NETWORK_UNREACHABLE = 0
    HOST_UNREACHABLE = 1


def receive_ping(sock: socket, packet_id: int, seq: int, time_sent: float, timeout: float) \
        -> tuple[float, Optional[Error]]:
    time_left = timeout
    while time_left > 0:
        select_time = time()
        ready = select([sock], [], [], time_left)
        select_time = time() - select_time
        if not ready[0]:
            break
        rec_packet, _ = sock.recvfrom(1024)
        time_received = time()
        icmp_type, icmp_code, other = parse_icmp(rec_packet)
        if icmp_type == 0 and icmp_code == 0:
            checksum, p_id, s, data = other
            data_for_checksum = struct.pack(ICMP_ECHO_FORMAT, icmp_type, icmp_code, 0, p_id, s) + data
            if p_id == packet_id and \
                    s == seq and \
                    validate_checksum(data_for_checksum, checksum):
                return time_received - time_sent, None
        elif icmp_type == 3:
            _, _, (_, p_id, s, _) = parse_icmp(other)
            if p_id == packet_id and s == seq:
                return time_received - time_sent, Error(icmp_code)
        time_left -= select_time
    return time() - time_sent, Error.TIMEOUT


def ping(sock: socket, host: str, timeout: float = 1) -> tuple[float, Optional[Error], int]:
    global current_seq
    packet_id = os.getpid() & 0xffff
    seq = current_seq
    current_seq += 1
    packet = build_echo_packet(packet_id, seq)
    sent = len(packet)
    sock.sendto(packet, (host, 1))
    rtt, err = receive_ping(sock, packet_id, seq, time(), timeout)
    return rtt, err, sent


class Statistics:

    def __init__(self):
        self.min_rtt = None
        self.max_rtt = None
        self.avg_rtt = None
        self.cnt = 0
        self.success_cnt = 0

    def update(self, rtt: Optional[int]):
        self.cnt += 1
        if rtt is not None:
            self.success_cnt += 1
            if self.min_rtt is None:
                self.min_rtt = self.max_rtt = self.avg_rtt = rtt
            else:
                self.min_rtt = min(self.min_rtt, rtt)
                self.max_rtt = max(self.max_rtt, rtt)
                self.avg_rtt = self.avg_rtt + (rtt - self.avg_rtt) / self.success_cnt

    def __str__(self):
        return f'Packages sent: {self.cnt}, Packages received: {self.success_cnt} ' \
               f'({100 * (self.cnt - self.success_cnt) / self.cnt}% loss)' \
               f'\n' \
               f'Min rtt: {self.min_rtt}, Max rtt: {self.max_rtt}, Avg rtt: {self.avg_rtt}'


def main(args):
    sock = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP)
    timeout = args.w / 1000
    stat = Statistics()
    if args.i is not None:
        sock.setsockopt(IPPROTO_IP, IP_TTL, args.i)
    try:
        host = gethostbyname(args.host)
        for _ in range(args.n):
            rtt_sec, err, sent = ping(sock, host, timeout)
            if err is None:
                rtt = round(rtt_sec * 1000)
                print(f'rtt: {rtt}ms, sent: {sent}b')
                stat.update(rtt)
            else:
                print('Error:', err.name)
                stat.update(None)
            if rtt_sec is not None and rtt_sec < 1:
                sleep(1 - rtt_sec)
        print(stat)
    except gaierror:
        print('Unknown host')
    sock.close()


parser = argparse.ArgumentParser(description='Ping')
parser.add_argument('-n', default=10, help='number of requests', type=int)
parser.add_argument('-w', default=1000, help='timeout, ms', type=int)
parser.add_argument('-i', default=None, help='TTL', type=int)
parser.add_argument('host', help='host address', type=str)
parsed_args = parser.parse_args()
main(parsed_args)
