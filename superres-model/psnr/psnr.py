import math
import numpy as np

# Calculate PSNR of two images
# @params (original_image, imitation_image)
# @ret float
def calc_psnr_numpy(org, imit):
  MAXVAL = 255
  err = imit - org
  sqerr = err**2
  dim = org.shape[0]*org.shape[1]*3
  mse = np.sum(sqerr)/dim
  psnr = 10 * math.log10(MAXVAL**2 / mse)
  return psnr

# Calculate PSNR of two images given their file paths
# Depends on calc_psnr_numpy
# @params (original_image_path, imitation_image_path)
# @ret float
def calc_psnr_file_path(org, imit):
  from PIL import Image
  img1  = Image.open(org)
  img2  = Image.open(imit)

  img1 = np.array(img1)
  img2 = np.array(img2)

  return calc_psnr_numpy(img1, img2)
