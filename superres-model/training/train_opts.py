DATA_LOADER = {
    'prefetch_size': 128,
    'shuffle_multiplier': 500,
    'num_parallel': 6,
}

MODEL = {
    'summarize_weights': True,
}

TRAIN = {
    'checkpoint_dir': 'ckpts',
    'checkpoint_file': 'ckpts/model.ckpt',
    'log_dir': 'train-log',
    'log_every': 25,
    'model_dir': 'saved-model',
    'print_every': 1,
    'save_every': 100,
    'show_detailed_losses': True,
}

LOSS = {
    'l2_mult': 0.0,
    'discr_l2_mult': 0.0,
    'adversarial_mult': 0.0,
    'mse_mult': 0.1,
    'perceptual_mult': 0.9
}

# scope names to separate related variables
MODEL_SCOPE = 'model'
DISCR_SCOPE = 'discriminator'

# loss names to separate losses
MODEL_LOSSES = 'model-losses'
DISCR_LOSSES = 'discr-losses'

