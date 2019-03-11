import tensorflow as tf

from losses import get_regularizer, get_discr_regularizer

def srcnn_x2_weak(lr_batch):
    # apply nn resize (as it is supported in TFLite)
    hr_shape = 2 * tf.shape(lr_batch)[1:3]
    lr_resized = tf.image.resize_nearest_neighbor(lr_batch, hr_shape)
    # save the name of the resized_image for model modification
    lr_resized = tf.identity(lr_resized, name='resized_image')
    # add a summary for resized
    tf.summary.image('scaled_image', lr_resized)
    # first conv. layer
    c1 = tf.layers.conv2d(lr_resized, 64, 9, padding='SAME', 
            activation=tf.nn.relu, kernel_regularizer=get_regularizer())
    # second conv. layer
    c2 = tf.layers.conv2d(c1, 32, 1, padding='SAME', 
            activation=tf.nn.relu, kernel_regularizer=get_regularizer())
    # final conv. layer
    hr_batch = tf.layers.conv2d(c2, 3, 5, padding='SAME', 
            kernel_regularizer=get_regularizer())
    return hr_batch

def srgan_discr_block(input_batch, num_filters, kernel_size, strides):
    conv_out = tf.layers.conv2d(input_batch, num_filters, kernel_size, strides, padding='SAME',
                                kernel_regularizer=get_discr_regularizer())
    bn_out = tf.layers.batch_normalization(conv_out)
    lr_out = tf.nn.leaky_relu(bn_out)
    return lr_out

def srgan_discriminator(input_batch):
    # first conv layer
    c1 = tf.layers.conv2d(input_batch, 64, 3, padding='SAME',
                          activation=tf.nn.leaky_relu, 
                          kernel_regularizer=get_discr_regularizer())
    cend = srgan_discr_block(c1, 64, 3, 2)
    # six srgan blocks as shown in the paper
    for nfilters in (128, 256, 512):
        cmid = srgan_discr_block(cend, nfilters, 3, 1)
        cend = srgan_discr_block(cmid, nfilters, 3, 2)
    # first dense layer with leaky relu
    d1 = tf.layers.dense(cend, 1024, activation=tf.nn.leaky_relu, 
                         kernel_regularizer=get_discr_regularizer())
    # final layer dense, leave sigmoid activation to loss
    d2 = tf.layers.dense(cend, 1, activation=None,
                         kernel_regularizer=get_discr_regularizer())
    return d2
