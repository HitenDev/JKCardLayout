# JKCardLayout
本Demo使用RecyclerView和自定义LayoutManager等，高仿即刻App探索页，支持卡片拖拽，卡片回退栈管理，下拉展示菜单等功能；欢迎大家点赞或者吐槽。

代码大部分使用Kotlin语言编写，假装娴熟，如有使用不当还请各路大神指点。

# 下载Demo
![](https://www.pgyer.com/app/qrcode/HjLN)

下载地址：https://www.pgyer.com/HjLN

# 图片展示

**视频演示**：https://youtu.be/F9QJEQJnHmo

![](https://upload-images.jianshu.io/upload_images/869487-de188885d2757ce3.gif?imageMogr2/auto-orient/strip)
![](https://upload-images.jianshu.io/upload_images/869487-6e5093c7c0955bcc.gif?imageMogr2/auto-orient/strip)
![](https://upload-images.jianshu.io/upload_images/869487-dc1eeb156de14d2e.gif?imageMogr2/auto-orient/strip)
![](https://upload-images.jianshu.io/upload_images/869487-5b26f5d84bbbb2e9.png?imageMogr2/auto-orient/strip)
![](https://upload-images.jianshu.io/upload_images/869487-9964307c9145ba73.png?imageMogr2/auto-orient/strip)


# 基本使用

## 卡片布局辅助类CardLayoutHelper

### 绑定RecyclerView

```Kotlin
mCardLayoutHelper = CardLayoutHelper()
mCardLayoutHelper.attachToRecyclerView(recycler_view)
```
### 绑定数据源List

```Kotlin
mCardLayoutHelper.bindDataSource(object : CardLayoutHelper.BindDataSource{
    override fun bind(): List<Any> {
        return list
    }
})
```
绑定数据源采用回调接口形式，需要返回绑定的RecyclerView对应Adapter下的数据源List


### 卡片参数配置

```Kotlin
 mCardLayoutHelper.setConfig(CardLayoutHelper.Config(maxCount = 2,offset = 12.dp,duration = 300,swipeThreshold = 0.4f))
```
CardLayoutHelper.Config接受参数配置，主要参数含义:
- maxCount    //卡片布局最多包含卡片个数，默认是2个
- offset    //卡片之间的偏移量，单位是像素
- duration    //卡片动画执行时间
- swipeThreshold    //拖拽卡片触发移除的阈值

### 操作Back和Next

```Kotlin
//check and doNext
if (mCardLayoutHelper.canNext()) {
    mCardLayoutHelper.doNext()
}
//check and doBack
if (mCardLayoutHelper.canBack()){
    mCardLayoutHelper.doBack()
}

//check noBack
if (mCardLayoutHelper.noBack()){
    super.onBackPressed()
}

```

### 回调监听

```Kotlin
mCardLayoutHelper.setOnCardLayoutListener(object :OnCardLayoutListener{
    override fun onSwipe(dx: Float, dy: Float) {
        Log.d("onStateChanged","dx:$dx dy:$dy")
    }
    override fun onStateChanged(state: CardLayoutHelper.State) {
        Log.d("onStateChanged",state.name)
    }
})
```
-  onSwipe(dx: Float, dy: Float) //卡片滑动距离回调
-  onStateChanged(state: CardLayoutHelper.State)//卡片状态监听，State详解
-  State.IDLE //默认状态，无拖拽和动画执行
-  State.SWIPE　//手指拖动状态
-  State.BACK_ANIM //Back动画执行中，包含两种情况(释放手势卡片缓慢回到默认位置过程、调用back方法执行动画)
-  State.LEAVE_ANIM　//LEAVE动画执行中，包括两种情况(释放手势卡片缓慢移除布局过程、调用next方法执行动画)

## 对下拉展示和Touch事件感兴趣的同学，直接看这个类：[PullDownLayout](https://github.com/HitenDev/JKCardLayout/blob/master/app/src/main/java/me/hiten/jkcardlayout/PullDownLayout.kt)


# 声明
本项目是个人作品，仅用作技术学习和开源交流，涉及到即刻APP相关的接口数据和图片资源禁止用于任何商业用途；如有疑问，请联系我。
