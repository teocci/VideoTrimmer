
## VideoTrimmer

[![Twitter][1]][2]

VideoTrimmer is a very simple and straight-forward Android app implementation that trim videos using [FFmpeg Android Java][3].

### Disclaimer

This repository contains a simple sample code intended to demonstrate the capabilities of [FFmpeg Android Java][3]. It is not intended to be used as-is in applications as a library dependency, and will not be maintained as such. Bug fix contributions are welcome, but issues and feature requests will not be addressed.

### Summary

VideoTrimmer has a trimming process that often involves a compression process. These process are implemented in Java to maintain compatibility with Android environments. However, it can achieve high compression ratio, and high compression speed, and at the same time guarantee the quality of the video. While the FFmpeg library has a small footprint size.

### Pre-requisites
    
- Android SDK 27
- Android Build Tools v27.0.3
- Android Support Repository

## Credits

* [FFmpeg Android Java][3] is an Android java library for FFmpeg binary compiled with x264, libass, fontconfig, freetype, fribidi and LAME.
* [Picasso][4]

## License

The code supplied here is covered under the MIT Open Source License.


  [1]: https://img.shields.io/badge/Twitter-@Teocci-blue.svg?style=flat
  [2]: http://twitter.com/teocci
  [3]: http://writingminds.github.io/ffmpeg-android-java/
  [4]: http://square.github.io/picasso/