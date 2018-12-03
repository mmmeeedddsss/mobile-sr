#! /usr/bin/python

import sys
import subprocess
datasets = ['Set5', 'Set14', 'BSDS100']

def cmd_for_downscale(file_path):
  new_path = file_path[:-4] + '_LR.png'
  cmd = 'convert ' + file_path + ' -scale 50% ' + new_path
  return cmd

def create_low_res(path, set5_flag=True, set14_flag=True, bsd100_flag=True):
  set5_path = path + '/Set5/'
  set14_path = path + '/Set14/'
  bsd100_path = path + '/BSDS100/'
  set5_range = (1, 6)
  set14_range = (1, 15)
  bsd100_range = (1, 101)
  if set5_flag:
    for i in range(*set5_range):
      subprocess.call(cmd_for_downscale(set5_path+'{}.png'.format(i)), shell=True)
  if set14_flag:
    for i in range(*set14_range):
      subprocess.call(cmd_for_downscale(set14_path+'{}.png'.format(i)), shell=True)
  if bsd100_flag:
    for i in range(*bsd100_range):
      subprocess.call(cmd_for_downscale(bsd100_path+'{}.png'.format(i)), shell=True)

if __name__ == '__main__':
  path = sys.argv[1]
  create_low_res(path)

