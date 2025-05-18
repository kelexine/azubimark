FROM gitpod/workspace-full-vnc

USER gitpod

# Install dependencies
RUN sudo apt-get update && \
    sudo apt-get install -y \
    libxtst6 \
    xauth \
    x11-utils \
    xvfb \
    fluxbox \
    dbus-x11 \
    xfce4 \
    xfce4-terminal \
    libnss3 \
    libnspr4 \
    libxss1 \
    libasound2 \
    libatk-bridge2.0-0 \
    libgtk-3-0 \
    libgdk-pixbuf2.0-0 \
    libdrm2 \
    libgbm1 \
    fonts-noto-color-emoji \
    mesa-utils \
    libgl1-mesa-dri

# Install OpenJDK 17 only and remove any other Java versions
RUN sudo apt-get purge -y openjdk-* && \
    sudo apt-get install -y openjdk-17-jdk && \
    sudo update-alternatives --set java $(update-alternatives --list java | grep "java-17")

# Install Android SDK
ENV ANDROID_HOME=/home/gitpod/android-sdk
ENV ANDROID_SDK_ROOT=${ANDROID_HOME}
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator

RUN mkdir -p ${ANDROID_HOME} && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d /tmp && \
    mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    mv /tmp/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Install Gradle 8.5
ENV GRADLE_VERSION=8.5
RUN wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O /tmp/gradle.zip && \
    sudo unzip -q /tmp/gradle.zip -d /opt && \
    sudo ln -s /opt/gradle-${GRADLE_VERSION} /opt/gradle && \
    sudo rm /tmp/gradle.zip

ENV PATH=${PATH}:/opt/gradle/bin

# Install Google Chrome
RUN wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb -O /tmp/chrome.deb && \
    sudo apt-get install -y /tmp/chrome.deb && \
    sudo rm /tmp/chrome.deb

# Create VNC script
RUN echo '#!/bin/bash\nstartxfce4 &\nvncserver -geometry 1920x1080 -depth 24 -name "Gitpod Android Development" :1' > /home/gitpod/start-vnc-session.sh && \
    chmod +x /home/gitpod/start-vnc-session.sh

# Setup environment variables for Android SDK
RUN echo 'export ANDROID_HOME=/home/gitpod/android-sdk' >> /home/gitpod/.bashrc && \
    echo 'export PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator' >> /home/gitpod/.bashrc

# Cleanup
RUN sudo apt-get clean && \
    sudo rm -rf /var/lib/apt/lists/*
