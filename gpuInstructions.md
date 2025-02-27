In order to get AIMs working with your GPU, you must have an Nvidia GPU with CUDA support. You can check if your GPU is supported by looking at the [CUDA GPUs](https://developer.nvidia.com/cuda-gpus) page.

If you have a supported GPU, you can follow the instructions below to install the necessary software.
### Installing CUDA and cuDNN
1. [Nvidia CUDA Toolkit 12.8](https://developer.nvidia.com/cuda-downloads)
2. Verify installation by running the following command in the terminal:
   ```bash
   nvcc --version
   ```
   This should return the version of CUDA installed on your system.
3. [Nvidia cuDNN with CUDA version 12](https://developer.nvidia.com/cudnn)

### Adding cuDNN to CUDA
1. Open the directory `User/Program Files/NVIDIA GPU Computing Toolkit/CUDA/`
2. Unzip the cuDNN archive and copy the contents of the `bin`, `include`, and `lib` folders into the corresponding folders in the CUDA directory.

### Enabling GPU support in AIMs
1. Open the AIMs program.
2. Click on the "Settings" button on the bottom bar.
3. Navigate to the `Advanced` tab.
4. Check the box next to `Enable GPU support`.
5. Select the GPU you want to use from the dropdown menu.
6. Click on the `Apply` button to save the changes.

If either CUDA or cuDNN is not installed, AIMs will automatically fall back to CPU mode.