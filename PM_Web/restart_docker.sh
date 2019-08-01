#!/bin/bash

sudo docker kill main_container
sudo docker rm main_container
sudo docker build -t main_image .
sudo docker run -d --name main_container -p 80:80 main_image
