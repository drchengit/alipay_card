# 仿支付宝银行卡页面效果

## 效果：
* 选择银行卡会打开银行卡
* 其他银行卡折叠到底部
* 不相关的view隐藏
* 再次点击还原

![](https://user-gold-cdn.xitu.io/2019/12/20/16f213d5811e4da5?w=320&h=515&f=gif&s=1458791)
## 实现：


![](https://user-gold-cdn.xitu.io/2019/12/20/16f214c242f377ab?w=320&h=515&f=gif&s=920243)
## 实现思路：
### 表面分析：
* 卡片是个列表，可以整体滑动，要用RecyclerView
* 迷之动画+大量的滑动冲突，常规的recyclerView无法满足，要自定义ReyclerView
* 看阵仗要能对卡片点击后用属性动画进行操作。（``选中的卡片上移，前三个卡片，下移折叠``）
* 要对打开状态进行判断，显示、隐藏顶部和其他view，控制列表是否可以滑动之类。
### 实际思路
网上有类似的开源库：https://github.com/loopeer/CardStackView
* 首先写一个**CardStackView**继承**viewGorup**实现reyclerView功能
* 在**onLayout**中实现折叠卡片和其他的View的个性布局
* 点击后对当前状态判断，调用**AnimatorAdapter**执行属性动画将卡片移动到对应位置，并处理卡片之外的view的显示状态。

我的博客： https://juejin.im/post/5dfc387e51882512657bb554
