import tensorflow as tf

def create_loss_layer(input_hr_batch, output_hr_batch, img_loss_fn):
    # add the image comparison loss
    cmp_loss = img_loss_fn(input_hr_batch, output_hr_batch)
    tf.losses.add_loss(cmp_loss)
    # ...
    for l in tf.losses.get_losses():
        print(l)
    total_loss = tf.losses.get_total_loss(add_regularization_losses=False)
    return total_loss

def mse_loss(input_hr_batch, output_hr_batch):
    return tf.nn.l2_loss(input_hr_batch - output_hr_batch) 

def mse_loss_layer(input_hr_batch, output_hr_batch):
    return create_loss_layer(input_hr_batch, output_hr_batch, mse_loss)
