#!/usr/bin/python3
import socket
import os
import time
import subprocess
import sys
import threading
import hashlib
from argparse import ArgumentParser
from queue import Queue

import sr

# default values
# can be changed from command line
# arguments when running
ip = '0.0.0.0'
port = 61275

VERBOSE=False

sr_queue = Queue()

# argument parser
def parse_arguments():
  parser = ArgumentParser()
  parser.add_argument(
    '--bind',
    help='bind to address')
  parser.add_argument(
    '--port',
    help='bind to port')
  parser.add_argument(
    '--verbose', action='store_true',
    help='enable verbose mode')
  parser.add_argument(
    '--model', required=True,
    help='specify the model path')
  args = parser.parse_args()
  return args

def submit(img_data):
  sr_queue.put(img_data)

def lookup(md5):
  try:
    img_file = open(md5, 'rb')
    data = img_file.read()
    img_file.close()
    return data
  except:
    return None

def save_to_fs(hr_img, md5):
  with open(md5, 'wb') as f:
    f.write(hr_img)

# SR processing
# given low-res image data
# and model path
# return high-res image data
def process(model):
  while True:
    lr_img = sr_queue.get()
    md5 = hashlib.md5(lr_img).hexdigest()
    hr_img = sr.apply_sr(lr_img, model)
    save_to_fs(hr_img, md5)

# logger function
# active if VERBOSE is defined
def log(s):
  if VERBOSE:
    print(s)

# handles single request from client
# returns True if client sends special
# end of transmission message at the end
# returns False if client has more
# data to send
# exception to that: single-image mode
def handle_request(clientSock):
  request_type = int(clientSock.recv(1))
  if request_type == 0:
    handle_new_request(clientSock)
  elif request_type == 1:
    handle_prev_req(clientSock)
  else:
    print('Not recognized message')
    sys.exit(1)

def handle_new_request(clientSock):
  imageSize = int(clientSock.recv(10))
  log('Reading ' + str(imageSize) + ' bytes of data...')

  # Get data from client
  imageData = b''
  sizetoread = imageSize
  readData = clientSock.recv(sizetoread)
  while True:
    imageData += readData
    readSize = len(imageData)
    if readSize == imageSize:
      break
    sizetoread = imageSize-readSize
    readData = clientSock.recv(sizetoread)
  log(str(len(imageData)) + ' bytes read.')

  # apply SR on file
  log('Submitting job...')
  hr_data = submit(imageData)
  log('Job submitted.')
  clientSock.close()

def handle_prev_req(clientSock):
  # get MD5 sum of image
  md5 = clientSock.recv(32)
  
  response = lookup(md5)
  if response:
    # send file back
    log('Sending SR image...')
    hr_size = str(len(response))
    log('Sending ' + hr_size + ' bytes of data')
    clientSock.send(response)
    log('File sent.')
  else:
    log('Not found.')
    clientSock.send(b'\x00')
  clientSock.close()

if __name__ == '__main__':
  args = parse_arguments()
  if args.bind:
    ip = args.bind
  if args.port:
    port = int(args.port)
  if args.verbose:
    VERBOSE = args.verbose
  model_path = args.model
  addr = (ip, port)

  # configure socket
  sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
  sock.bind(addr)
  sock.listen(1)

  print('Listening on: ' + str(addr))

  threading.Thread(target=process, args=(model_path,)).start()

  while True:
    clientSock, clientAddr = sock.accept()
    req_handler_t = threading.Thread(target=handle_request, args=(clientSock,))
    req_handler_t.start()

  print('Closing connection...')
  clientSock.close()
  sock.close()
  sys.exit()
