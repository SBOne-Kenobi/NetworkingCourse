import argparse
import socket
from datetime import datetime
import json
from urllib.parse import urlparse

from http_parser.http import *

import requests

CONFIG_FILE = 'config.json'

crlf = '\r\n'
OK_HEADER = 'HTTP/1.1 200 OK'
CONTENT_TYPE_TEXT = 'Content-Type: text/html'

NOT_FOUND_HEADER = 'HTTP/1.1 404 Not Found'
NOT_FOUND_HTML = '<HTML><HEAD><TITLE>Not Found</TITLE></HEAD> <BODY>Not Found</BODY></HTML>'
NOT_FOUND = NOT_FOUND_HEADER + crlf + CONTENT_TYPE_TEXT + crlf + crlf + NOT_FOUND_HTML

BANNED_HEADER = 'HTTP/1.1 403 Forbidden'
BANNED_HTML_TEMP = '<HTML><HEAD><TITLE>Forbidden</TITLE></HEAD> <BODY>{} is banned</BODY></HTML>'
BANNED_TEMP = BANNED_HEADER + crlf + CONTENT_TYPE_TEXT + crlf + crlf + BANNED_HTML_TEMP

BAD_REQUEST_HEADER = 'HTTP/1.1 400 Bad Request'
BAD_REQUEST_HTML = '<HTML><HEAD><TITLE>Bad Request</TITLE></HEAD> <BODY>Bad Request</BODY></HTML>'
BAD_REQUEST = BAD_REQUEST_HEADER + crlf + CONTENT_TYPE_TEXT + crlf + crlf + BAD_REQUEST_HTML

TAG_RESPONSE_GET = 'RESPONSE_GET'
TAG_RESPONSE_POST = 'RESPONSE_POST'

class Logger:
    def __init__(self, file):
        self.file = open(file, 'w')

    def __del__(self):
        self.file.close()

    def log(self, tag, s):
        self.file.write('{} | {} | {}\n'.format(
            datetime.now().strftime("%d/%m/%Y %H:%M:%S"),
            tag, s
        ))
        self.file.flush()


logger = Logger('proxy.log')
black_list = set()


def get_host(url):
    host = urlparse(url).hostname
    if 'www' in host:
        host = host[4:]
    return host


def load_config():
    global black_list
    with open(CONFIG_FILE, 'r') as f:
        config = json.load(f)
    black_list = set(config['black_list'])


def process_get(url):
    resp = requests.get(url)
    logger.log(TAG_RESPONSE_GET, f'{url} {resp.status_code}')
    if resp.status_code == 404:
        resp = NOT_FOUND
    else:
        resp = OK_HEADER + crlf + crlf + resp.content.decode(resp.encoding)
    return resp


def process_post(url, body):
    resp = requests.post(url, body)
    logger.log(TAG_RESPONSE_POST, f'{url} {resp.status_code}')
    if resp.status_code == 404:
        resp = NOT_FOUND
    else:
        resp = OK_HEADER + crlf + crlf + resp.content.decode(resp.encoding)
    return resp


def process_request(request: HttpParser, body):
    method = request.get_method().lower()
    url = 'http://' + request.get_path()[1:]
    host = get_host(url)
    if host in black_list:
        resp = BANNED_TEMP.format(host)
    elif method == 'get':
        resp = process_get(url)
    elif method == 'post':
        resp = process_post(url, body)
    else:
        resp = BAD_REQUEST
    return resp


def parse_request(conn):
    request = HttpParser()
    body = []
    while True:
        data = conn.recv(1024)
        if not data:
            break
        recved = len(data)
        nparsed = request.execute(data, recved)
        assert nparsed == recved
        if request.is_partial_body():
            body.append(request.recv_body().decode('utf-8'))
        if request.is_message_complete():
            break
    return request, ''.join(body)


def process_client(conn):
    request, body = parse_request(conn)
    resp = process_request(request, body)
    conn.send(resp.encode())
    conn.close()


def init_server(port):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.bind(('', port))
    sock.listen()
    print('Server started!')
    return sock


def start_server(port):
    sock = init_server(port)
    while True:
        try:
            conn, addr = sock.accept()
            print('Connected:', addr)
            process_client(conn)
        except KeyboardInterrupt:
            print('Server is shutting down...')
            break


parser = argparse.ArgumentParser()

parser.add_argument('--max_conn', help='Maximum allowed connections', default=4, type=int)
parser.add_argument('--port', help='Number of port', default=8080, type=int)

args = parser.parse_args()

load_config()
max_connection = args.max_conn
port = args.port

print('Server will be started on ', port)
start_server(port)
