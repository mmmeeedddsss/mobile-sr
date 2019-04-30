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

# initialize the queue
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

# put the data to end of the queue
def submit(img_data):
  sr_queue.put(img_data)

# check whether the file with given MD5
# is ready
# returns the SR'd data if so,
# returns None if not
def lookup(md5):
  try:
    img_file = open(md5, 'rb')
    data = img_file.read()
    img_file.close()
    return data
  except:
    return None

# saves the SR'd image to the filesystem
# with the MD5 as the filename
# MD5 is the hash of LR image
def save_to_fs(hr_img, md5):
  with open(md5, 'wb') as f:
    f.write(hr_img)

# SR processing
# takes the model path as arg
# takes the LR image from the front of
# the queue, processes it and saves as
# a file naming it as its MD5
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

# handles a single request from client
# according to the type of the request
# 0 is new job submission
# 1 is result of a previous job submission
def handle_request(clientSock):
  request_type = int(clientSock.recv(1))
  if request_type == 0:
    handle_new_request(clientSock)
  elif request_type == 1:
    handle_prev_req(clientSock)
  else:
    print('Not recognized message')
    sys.exit(1)
  clientSock.close()

# reads a single image from the socket and
# puts it to the end of the queue
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

# reads the MD5 hash sum of an image
# from the socket and sends back its SR'd image
# if it's already processes
# sends a zero byte if it's not processed yet
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

  # start processing images from the queue
  threading.Thread(target=process, args=(model_path,)).start()

  while True:
    clientSock, clientAddr = sock.accept()
    req_handler_t = threading.Thread(target=handle_request, args=(clientSock,))
    req_handler_t.start()

  print('Closing connection...')
  sock.close()
  sys.exit()
