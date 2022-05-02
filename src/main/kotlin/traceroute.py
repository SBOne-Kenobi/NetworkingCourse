import argparse
import os
import socket
import struct
import sys
from socket import socket, IPPROTO_ICMP, gethostbyname, AF_INET, SOCK_RAW, gaierror, IPPROTO_IP, IP_TTL, getfqdn
from time import time
from typing import Optional

from select import select

ICMP_ECHO_REQUEST = 8
ICMP_ECHO_FORMAT = 'bbHHh'
ICMP_ECHO_HEADER_SIZE = 8

IP_HEADER_SIZE = 20


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
    if (icmp_type == 0 or icmp_type == 8) and icmp_code == 0:
        icmp_header = icmp_part[:ICMP_ECHO_HEADER_SIZE]
        data = icmp_part[ICMP_ECHO_HEADER_SIZE:]
        icmp_type, icmp_code, checksum, p_id, s = struct.unpack(ICMP_ECHO_FORMAT, icmp_header)
        return icmp_type, icmp_code, (checksum, p_id, s, data)
    elif icmp_type == 11:
        checksum = struct.unpack('H', icmp_part[2:4])[0]
        return icmp_type, icmp_code, (checksum, icmp_part[8:])
    else:
        return icmp_type, icmp_code, icmp_part[2:]


def receive_ping(sock: socket, packet_id: int, seq: int, time_sent: float, timeout: float) \
        -> tuple[Optional[float], Optional[str], bool]:
    time_left = timeout
    while time_left > 0:
        select_time = time()
        ready = select([sock], [], [], time_left)
        select_time = time() - select_time
        if not ready[0]:
            break
        rec_packet, (addr, _) = sock.recvfrom(1024)
        time_received = time()
        icmp_type, icmp_code, other = parse_icmp(rec_packet)
        if icmp_type == 0 and icmp_code == 0:
            checksum, p_id, s, data = other
            data_for_checksum = struct.pack(ICMP_ECHO_FORMAT, icmp_type, icmp_code, 0, p_id, s) + data
            if p_id == packet_id and \
                    s == seq and \
                    validate_checksum(data_for_checksum, checksum):
                return time_received - time_sent, addr, True
        elif icmp_type == 11:
            checksum, other = other
            _, _, (_, p_id, s, _) = parse_icmp(other)
            data_for_checksum = struct.pack('bbHxxxx', icmp_type, icmp_code, 0) + other
            if p_id == packet_id and \
                    s == seq and \
                    validate_checksum(data_for_checksum, checksum):
                return time_received - time_sent, addr, False
        time_left -= select_time
    return None, None, False


def trace(sock: socket, host: str, timeout: float, n: int):
    packet_id = os.getpid() & 0xffff
    ttl = 0
    seq = 0
    while True:
        ttl += 1
        print(ttl, end='\t')
        sock.setsockopt(IPPROTO_IP, IP_TTL, ttl)
        host_reached = False
        addr = None
        for _ in range(n):
            seq += 1
            packet = build_echo_packet(packet_id, seq)
            sock.sendto(packet, (host, 1))
            rtt, r_addr, is_host = receive_ping(sock, packet_id, seq, time(), timeout)
            if rtt is None:
                print('*', end='\t')
            else:
                print(round(rtt * 1000), 'ms', end='\t')
            sys.stdout.flush()
            addr = r_addr or addr
            if is_host:
                host_reached = True
        if addr is None:
            print('*')
        else:
            name = getfqdn(addr)
            if name == addr:
                print(addr)
            else:
                print(f'{name} [{addr}]')
        sys.stdout.flush()
        if host_reached:
            break


def main(args):
    sock = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP)
    timeout = args.w / 1000
    try:
        host = gethostbyname(args.host)
        trace(sock, host, timeout, args.n)
    except gaierror:
        print('Unknown host')
    sock.close()


parser = argparse.ArgumentParser(description='Trace route')
parser.add_argument('-w', default=1000, help='timeout, ms', type=int)
parser.add_argument('-n', default=3, help='number of requests', type=int)
parser.add_argument('host', help='host address', type=str)
parsed_args = parser.parse_args()
main(parsed_args)
