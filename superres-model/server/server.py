#!/usr/bin/python3
import socket
import os
import time
import subprocess
import sys
import threading
from argparse import ArgumentParser

import sr

# default values
# can be changed from command line
# arguments when running
ip = '0.0.0.0'
port = 61275

VERBOSE=False

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

# SR processing
# given low-res image data
# and model path
# return high-res image data
def process(lr_img, model):
  hr_img = sr.apply_sr(lr_img, model)
  return hr_img

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
def handle_request(clientSock, model):
  imageSize = int(clientSock.recv(10))
  if imageSize == 0:
    return True
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
  log('Superresolution started...')
  t = time.time()
  hr_data = process(imageData, model)
  t = time.time() - t
  log('Superresolution finished.')

  # send file back
  log('Sending SR image...')
  hr_size = str(len(hr_data))
  log('Sending ' + hr_size + ' bytes of data')
  clientSock.send(hr_data)

  log('File sent.')
  log('Time taken: ' + str(t))

  return False

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

  while True:
    clientSock, clientAddr = sock.accept()
    req_handler_t = threading.Thread(target=handle_request, args=(clientSock, model_path))
    req_handler_t.start()

  print('Closing connection...')
  clientSock.close()
  sock.close()
  sys.exit()
