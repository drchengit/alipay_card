package com.loopeer.cardstack;

import android.animation.ObjectAnimator;
import android.view.View;

/**
 * 这个类是属性动画具体实现类
 */
public class AllMoveDownAnimatorAdapter extends AnimatorAdapter {

    public AllMoveDownAnimatorAdapter(CardStackView cardStackView) {
        super(cardStackView);
    }

    /**
     * 打开动画
     */
    protected void itemExpandAnimatorSet(final CardStackView.ViewHolder viewHolder, int position) {
        final View itemView = viewHolder.itemView;
        viewHolder.beforeOpenHeight = itemView.getMeasuredHeight();//记录一下没有完全展开后的高度，恢复时要用
        viewHolder.itemView.clearAnimation();
        //选中卡片飞到顶部
        ObjectAnimator oa = ObjectAnimator.ofFloat(itemView, View.Y, itemView.getY(), mCardStackView.getScrollY() + mCardStackView.getPaddingTop());
        mSet.play(oa);
        int collapseShowItemCount = 0;

        //真实选择的childPosition
        int trueSelectChildPosition = mCardStackView.getmHeadView()==null?mCardStackView.getCardSelectPosition():mCardStackView.getCardSelectPosition()+1;
       //折叠到第几个；
        int collapsePosition = mCardStackView.getNumBottomShow();
        if(mCardStackView.getmHeadView()!=null) collapsePosition++;
        if(mCardStackView.getCardSelectPosition()<3)collapsePosition++;
        for (int i = 0; i < mCardStackView.getChildCount(); i++) {
            int childTop;
            if(mCardStackView.getmHeadView()!=null&&i==0)continue;
            if(mCardStackView.getmBottomView()!=null&&i==mCardStackView.getChildCount()-1)continue;
            if (i == trueSelectChildPosition) continue;
            //关于没选中部分item 的处理是 ， card取前三折叠，其他移到不可见区域
            final View child = mCardStackView.getChildAt(i);
            child.clearAnimation();

            if (i < collapsePosition) {
                childTop = mCardStackView.getShowHeight() - getCollapseStartTop(collapseShowItemCount) + mCardStackView.getScrollY();
                ObjectAnimator oAnim = ObjectAnimator.ofFloat(child, View.Y, child.getY(), childTop);
                mSet.play(oAnim);
                collapseShowItemCount++;
            } else {
                ObjectAnimator oAnim = ObjectAnimator.ofFloat(child, View.Y, child.getY(), mCardStackView.getShowHeight() + mCardStackView.getScrollY());
                mSet.play(oAnim);
            }
        }
    }

    /**
     * 关闭动画
     *  @param viewHolder  之前选中的viewHolder
     */
    @Override
    protected void itemCollapseAnimatorSet(CardStackView.ViewHolder viewHolder) {

        int childTop = mCardStackView.getPaddingTop();
        for (int i = 0; i < mCardStackView.getChildCount(); i++) {
            View child = mCardStackView.getChildAt(i);
            child.clearAnimation();
            final CardStackView.LayoutParams lp =
                    (CardStackView.LayoutParams) child.getLayoutParams();
            childTop += lp.topMargin;

                ObjectAnimator oAnim = ObjectAnimator.ofFloat(child, View.Y, child.getY(), childTop);
                mSet.play(oAnim);
                if(mCardStackView.getChildSeletPosition()==i&&mCardStackView.needOpen(i)){//选中itemView 测量的高度是完全展开后的高度，我只需要没展开后的高度
                    childTop += viewHolder.beforeOpenHeight;

                }else {
                    childTop +=lp.mHeight;
                }

        }
    }

}
