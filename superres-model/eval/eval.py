#! /usr/bin/python

import sys
import subprocess
datasets = ['Set5', 'Set14', 'BSDS100']

def cmd_for_downscale(file_path):
  new_path = file_path[:-4] + '_LR.png'
  cmd = 'convert ' + file_path + ' -scale 50% ' + new_path
  return cmd

if __name__ == '__main__':
  path = sys.argv[1]
  set5_path = path + '/Set5/'
  set14_path = path + '/Set14/'
  bsd100_path = path + '/BSDS100/'
  set5_range = (1, 6)
  set14_range = (1, 15)
  bsd100_range = (1, 101)
  for i in range(*set5_range):
    subprocess.call(cmd_for_downscale(set5_path+'{}.png'.format(i)), shell=True)
  for i in range(*set14_range):
    subprocess.call(cmd_for_downscale(set14_path+'{}.png'.format(i)), shell=True)
  for i in range(*bsd100_range):
    subprocess.call(cmd_for_downscale(bsd100_path+'{}.png'.format(i)), shell=True)

