# Project Plan

AI LLM Gallery to try models on device vision + text. The app should allow users to interact with on-device LLMs (like Gemini Nano via Google AI Edge SDK or similar) for both text-to-text and vision-to-text (image captioning, object detection, etc.) tasks. It should have a gallery-like interface to showcase different models and their capabilities.

## Project Brief

# Project Brief: AI LLM Gallery

A mobile platform designed to showcase and interact with on-device Large Language Models (LLMs). This app enables users to explore multimodal AI capabilities—ranging from text generation to visual analysis—entirely on the device, ensuring privacy and low-latency performance.

## Features
*   **Model Capability Gallery**: A visually rich dashboard showcasing various on-device AI models (e.g., Gemini Nano) categorized by their strengths, such as "Creative Writing," "Image Description," or "Object Detection."
*   **Multimodal Chat Interface**: A seamless interaction screen where users can engage in text-to-text conversations or upload images for vision-to-text tasks (captioning, visual Q&A).
*   **On-Device Vision Analysis**: A dedicated toolset for real-time image processing that identifies objects, extracts text, or generates descriptions without needing an internet connection.
*   **Performance Monitoring**: A minimalist overlay or detail view showing inference speed and hardware utilization for each AI task.

## High-Level Technical Stack
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose with Material Design 3 (M3)
*   **Navigation**: Jetpack Navigation 3 (State-driven)
*   **Adaptive Strategy**: Compose Material Adaptive Library (supporting handsets, foldables, and tablets)
*   **AI Engine**: Google AI Edge SDK / MediaPipe (for on-device LLM and Vision inference)
*   **Concurrency**: Kotlin Coroutines & Flow for non-blocking AI processing
*   **Image Handling**: Coil for optimized image loading and transformation

## Implementation Steps

### Task_1_Base_Infrastructure: Initialize the project with Material 3, Navigation 3, and necessary AI dependencies (MediaPipe/Google AI Edge). Set up the base theme with dynamic color support and the main navigation shell.
- **Status:** IN_PROGRESS
- **Updates:** Starting Task 1: Setting up base infrastructure, dependencies, and navigation.
1. Adding MediaPipe dependencies to libs.versions.toml.
2. Updating app/build.gradle.kts.
3. Setting up Material 3 theme and Navigation 3 shell.
- **Acceptance Criteria:**
  - Project builds successfully
  - Material 3 theme with dynamic color is configured
  - Navigation 3 shell is functional
  - Necessary AI SDK dependencies are added to build.gradle

### Task_2_Capability_Gallery: Build the adaptive 'Model Capability Gallery' dashboard showcasing various AI tasks (Text Generation, Vision Analysis). Use Compose Material Adaptive for multi-pane support and ensure full edge-to-edge display.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Dashboard UI displays capability cards
  - Layout adapts to different screen sizes (handset, foldable, tablet)
  - Full edge-to-edge implementation is verified

### Task_3_Multimodal_AI_Chat: Implement the Multimodal Chat interface and integrate the on-device AI engine (MediaPipe LLM Inference) for text-to-text and vision-to-text tasks. Include performance monitoring for inference speed and hardware utilization.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Text-to-text chat generates responses on-device
  - Vision-to-text handles image inputs and generates descriptions
  - Inference speed/latency is displayed in the UI
  - Coil is used for optimized image handling

### Task_4_Final_Polish_Verification: Create an adaptive app icon, finalize UI refinements for a vibrant energetic look, and perform a final verification of the application stability and requirement alignment.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Adaptive app icon created and functional
  - App follows Material Design 3 guidelines with vibrant colors
  - No crashes during multimodal AI tasks
  - Final build passes and app is stable

