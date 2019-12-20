package com.loopeer.cardstack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.view.animation.AccelerateDecelerateInterpolator;


/**
 * 点击后，这个类对卡片进行属性动画操作
 */
public abstract class AnimatorAdapter {
    static final int ANIMATION_DURATION = 400;

    protected CardStackView mCardStackView;
    protected AnimatorSet mSet;

    public AnimatorAdapter(CardStackView cardStackView) {
        mCardStackView = cardStackView;
    }

    protected void initAnimatorSet() {
        mSet = new AnimatorSet();
        mSet.setInterpolator(new AccelerateDecelerateInterpolator());
        mSet.setDuration(getDuration());
    }




    protected abstract void itemExpandAnimatorSet(CardStackView.ViewHolder viewHolder, int position);

    protected abstract void itemCollapseAnimatorSet(CardStackView.ViewHolder viewHolder);

    /**
     * 打开动画
     */
    public void onItemExpand(final CardStackView.ViewHolder viewHolder, int position) {
        if (mSet != null && mSet.isRunning()) return;
        initAnimatorSet();
        final int preSelectPosition = mCardStackView.getCardSelectPosition();
        final CardStackView.ViewHolder preSelectViewHolder = mCardStackView.getViewHolder(preSelectPosition);
        if (preSelectViewHolder != null) {
            preSelectViewHolder.onItemExpand(false);
        }
        mCardStackView.setCardSelectPosition(position);
        itemExpandAnimatorSet(viewHolder, position);
        mSet.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mCardStackView.setScrollEnable(false);
                if (preSelectViewHolder != null) {
                    preSelectViewHolder.onAnimationStateChange(CardStackView.ANIMATION_STATE_START, false);
                }
                viewHolder.onAnimationStateChange(CardStackView.ANIMATION_STATE_START, true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                viewHolder.onItemExpand(true);
                if (preSelectViewHolder != null) {
                    preSelectViewHolder.onAnimationStateChange(CardStackView.ANIMATION_STATE_END, false);
                }
                viewHolder.onAnimationStateChange(CardStackView.ANIMATION_STATE_END, true);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                if (preSelectViewHolder != null) {
                    preSelectViewHolder.onAnimationStateChange(CardStackView.ANIMATION_STATE_CANCEL, false);
                }
                viewHolder.onAnimationStateChange(CardStackView.ANIMATION_STATE_CANCEL, true);
            }
        });
        mSet.start();

    }


    /**
     * 关闭动画
     * @param viewHolder  之前选中的viewHolder
     */
    public void onItemCollapse(final CardStackView.ViewHolder viewHolder) {
        if (mSet != null && mSet.isRunning()) return;
        initAnimatorSet();
        itemCollapseAnimatorSet(viewHolder);
        mSet.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                viewHolder.onItemExpand(false);
                mCardStackView.setScrollEnable(true);
                viewHolder.onAnimationStateChange(CardStackView.ANIMATION_STATE_START, false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mCardStackView.setCardSelectPosition(CardStackView.DEFAULT_SELECT_POSITION);
                viewHolder.onAnimationStateChange(CardStackView.ANIMATION_STATE_END, false);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                viewHolder.onAnimationStateChange(CardStackView.ANIMATION_STATE_CANCEL, false);
            }
        });
        mSet.start();
    }

    protected int getCollapseStartTop(int collapseShowItemCount) {

        return mCardStackView.getOverlapGapsCollapse()
                * (mCardStackView.getNumBottomShow() - collapseShowItemCount);
    }

    public int getDuration() {
        return mCardStackView.getDuration();
    }
}
