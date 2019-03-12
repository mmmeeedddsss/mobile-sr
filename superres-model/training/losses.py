import tensorflow as tf
import train_opts as OPTS

## general losses 
def create_loss_layer(input_hr_batch, output_hr_batch, img_loss_fn, 
                      use_original=None, discr_output=None, discr_labels=None):
    # add the image comparison loss
    cmp_loss = img_loss_fn(input_hr_batch, output_hr_batch)
    tf.add_to_collection(OPTS.MODEL_LOSSES, cmp_loss)
    
    # sanity check
    all_none = use_original is None and discr_output is None and discr_labels is None
    all_sth = not (use_original is None or discr_output is None or discr_labels is None)
    assert all_none or all_sth, 'GAN parameters should either all be None or should all exist!'
    # add GAN losses
    discr_exists = discr_output is not None
    if discr_exists:
        # add adversarial loss
        adv_loss = adversarial_loss(use_original, discr_output)
        tf.add_to_collection(OPTS.MODEL_LOSSES, adv_loss)
        # add discriminator losses
        discr_loss = discriminator_losses(discr_output, discr_labels)
        tf.add_to_collection(OPTS.DISCR_LOSSES, discr_loss)

    # add regularization losses
    for loss in tf.losses.get_regularization_losses(OPTS.MODEL_SCOPE):
        tf.add_to_collection(OPTS.MODEL_LOSSES, loss)
    if discr_exists:
        for loss in tf.losses.get_regularization_losses(OPTS.DISCR_SCOPE):
            tf.add_to_collection(OPTS.DISCR_LOSSES, loss)

    # print losses
    print()
    print()
    print('LOSSES:')
    print('******************************')
    print('Printing generator losses:')
    for l in tf.get_collection(OPTS.MODEL_LOSSES):
        print(l)
    if discr_exists:
        print('******************************')
        print('Printing discriminator losses:')
        for l in tf.get_collection(OPTS.DISCR_LOSSES):
            print(l)
    print('******************************')
    print()
    print()

    # sum and return the losses
    total_loss = tf.reduce_sum(tf.get_collection(OPTS.MODEL_LOSSES))
    total_discr_loss = tf.reduce_sum(tf.get_collection(OPTS.DISCR_LOSSES)) if discr_exists else None
    return total_loss, total_discr_loss

def mse_loss(input_hr_batch, output_hr_batch):
    return tf.reduce_mean(tf.squared_difference(input_hr_batch, output_hr_batch), name='mse-loss')


## discriminator losses
def discriminator_losses(discr_output, discr_labels):
    # use sigmoid cross entropy loss
    loss = tf.reduce_mean(
        tf.nn.sigmoid_cross_entropy_with_logits(
            labels=discr_labels,
            logits=discr_output),
        name='discr-loss')
    return loss

def adversarial_loss(use_original, discr_output):
    # the generator network wants the discriminator to detect
    # the generated (fake) HR images as real, but only if
    # the generator was used as input and not the original dataset
    adv_loss = tf.cond(
        use_original, 
        lambda: 0.0,
        lambda: tf.reduce_mean(
                    tf.nn.sigmoid_cross_entropy_with_logits(
                        labels=tf.ones_like(discr_output),
                        logits=discr_output)))
    scaled_adv_loss = tf.multiply(OPTS.LOSS['adversarial_mult'], adv_loss,
                                  name='adversarial-loss')
    return scaled_adv_loss



## regularizers
def get_regularizer():
    return tf.contrib.layers.l2_regularizer(scale=OPTS.LOSS['l2_mult'])

def get_discr_regularizer():
    return tf.contrib.layers.l2_regularizer(scale=OPTS.LOSS['discr_l2_mult'])
