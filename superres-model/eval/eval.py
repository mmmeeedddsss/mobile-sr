#! /usr/bin/python

import sys
import subprocess
import psnr
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

def cmd_for_SR(file_path):
  cmd = 'python bilinear.py {}'.format(file_path)
  return cmd

def apply_SR(path, set5_flag=True, set14_flag=True, bsd100_flag=True):
  set5_path = path + '/Set5/'
  set14_path = path + '/Set14/'
  bsd100_path = path + '/BSDS100/'
  set5_range = (1, 6)
  set14_range = (1, 15)
  bsd100_range = (1, 101)
  if set5_flag:
    for i in range(*set5_range):
      subprocess.call(cmd_for_SR(set5_path+'{}_LR.png'.format(i)), shell=True)
  if set14_flag:
    for i in range(*set14_range):
      subprocess.call(cmd_for_SR(set14_path+'{}_LR.png'.format(i)), shell=True)
  if bsd100_flag:
    for i in range(*bsd100_range):
      subprocess.call(cmd_for_SR(bsd100_path+'{}_LR.png'.format(i)), shell=True)
  
def calc_psnr_values(path, set5_flag=True, set14_flag=True, bsd100_flag=True):
  set5_path = path + '/Set5/'
  set14_path = path + '/Set14/'
  bsd100_path = path + '/BSDS100/'
  set5_range = (1, 6)
  set14_range = (1, 15)
  bsd100_range = (1, 101)
  result = []
  if set5_flag:
    set5_sum = []
    for i in range(*set5_range):
      set5_sum.append(\ 
        psnr.calc_psnr_file_path(set5_path+'{}.png'.format(i),\ 
                                 set5_path+'{}_LR_interp'.format(i)))
    set5_mean = np.mean(set5_sum)
    result.append(set5_mean)
  if set14_flag:
    set14_sum = []
    for i in range(*set14_range):
      set14_sum.append(\ 
        psnr.calc_psnr_file_path(set14_path+'{}.png'.format(i),\ 
                                 set14_path+'{}_LR_interp'.format(i)))
    set14_mean = np.mean(set14_sum)
    result.append(set14_mean)
  if bsd100_flag:
    bsd100_sum = []
    for i in range(*bsd100_range):
      bsd100_sum.append(\ 
        psnr.calc_psnr_file_path(bsd100_path+'{}.png'.format(i),\ 
                                 bsd100_path+'{}_LR_interp'.format(i)))
    bsd100_mean = np.mean(bsd100_sum)
    result.append(bsd100_mean)
  return result

if __name__ == '__main__':
  path = sys.argv[1]
  create_low_res(path)
  apply_SR(path)

