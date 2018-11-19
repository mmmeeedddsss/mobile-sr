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
