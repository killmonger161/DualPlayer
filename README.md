DualPlayer
A professional-grade Android audio application built with Kotlin and Jetpack Media3 (ExoPlayer). This project features a unique dual-deck architecture allowing two independent audio streams to be controlled and mixed simultaneously.

Key Features
Dual-Deck Interface: Two fully independent playback modules (Deck 01 // LEFT and Deck 02 // RIGHT).

Independent Volume Control: Dedicated vertical-style seekbars for each deck to manage discrete gain levels.

Master Playback Control: A centralized master Play/Pause button that toggles both engines at the exact same timestamp.

Real-time Progress Tracking: Precision seekbars with monospace time displays (00:00) for both current position and total duration.

Stereo Channel Routing: Built-in ChannelMixer logic designed to route audio to specific hardware outputs.

Neon Dark Mode: A high-contrast black and neon (#00F2FF / #FF00E5) interface optimized for visibility.

Technical Implementation
Language: Kotlin

Audio Engine: androidx.media3.exoplayer

UI Components: RelativeLayout and LinearLayout with custom XML backgrounds.

State Management: Handler and Looper for synchronized UI updates every 500ms.

How to Run
Clone the Repo: git clone https://github.com/killmonger161/DualPlayer.git

Load Media: Tap the + (Load) button on either deck to select an audio file from your device.

Mix: Use the "VOLUME CONTROL" sliders to balance audio between Deck A and Deck B.

Sync: Tap the master Play button in the bottom bar to start/stop both tracks together.

License
This project is open-source and available under the MIT License.