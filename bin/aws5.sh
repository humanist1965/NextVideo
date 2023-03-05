#!/bin/bash

# script to start a ssh session on my Amazon EC2 instance that I run the App off
cd ~/AWS/KEYPAIRS

ssh -i "MK65_2023.pem" ubuntu@ec2-13-42-48-163.eu-west-2.compute.amazonaws.com



