
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

```
Tab icons
Asset size 32dp (128px)
Asset padding 2dp
Color #fff

Video item icons
Asset size 32dp (128px)
Asset padding 6dp
Color #707070 (gray icons)
Color #3681FF (pressed icons)
Color #fff (white icons)
Color #e43333 (red icons)

```
## License

The code supplied here is covered under the MIT Open Source License.


  [1]: https://img.shields.io/badge/Twitter-@Teocci-blue.svg?style=flat
  [2]: http://twitter.com/teocci
  [3]: http://writingminds.github.io/ffmpeg-android-java/
  [4]: http://square.github.io/picasso/
  [5]

### 项目介绍
实现Android上使用ffmpeg进行视频裁剪，压缩功能。类似视频裁剪功能的开源项目,个人觉得非常稀缺。
不像ios开源的那么多,自己在开发过程中也是不断的摸索,其中也遇到不少蛋疼的问题。
现在简单说一下这个项目实现。

### 使用到相关技术
* FFmpeg实现裁剪视频
* FFmpeg实现裁剪之后的视频压缩
* ContentResolver获取所有视频资源
* 采用VideoView播放视频
* 使用水平滚动的ListView显示视频的帧图片
* 通过MediaMetadataRetriever获取视频帧的Bitmap
* View的自定义

### 功能扩展思考
视频裁剪功能之后往往涉及到视频的压缩和上传,每一个功能都是Android开发中的高阶内容,比如说视频的压缩,压缩库其实开源的有一些,
但是能达到压缩比高、压缩速度快,同时又保证视频的质量,这样的开源库还是比较少的。
在这个项目中，我只是简单的实现了裁剪后的视频压缩，想达到一个好的压缩效果，还需要在项目中对视频压缩参数进行调整，
大家可以fork项目进行相应的移植和修改。

### 其他
视频裁剪完成，会将裁剪好的视频输出保存至应用的Android->data->包名->cache文件夹中
##### 欢迎star、fork和issues.

### License

See the [LICENSE](https://github.com/iknow4/Android-Video-Trimmer/blob/master/LICENSE) file.


<img src="https://github.com/iknow4/iknow.Images/blob/master/gif/videoTrim.gif?raw=true" width="400" height="700" alt="VideoTrim"/>
