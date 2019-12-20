package com.loopeer.test.cardstackview;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.loopeer.cardstack.CardStackView;
import com.loopeer.cardstack.StackAdapter;

public class TestStackAdapter extends StackAdapter<Integer> {

    public TestStackAdapter(Context context) {
        super(context);

    }


    @Override
    public void bindView(Integer data, int position, CardStackView.ViewHolder holder) {
        if (holder instanceof CardViewHolder) {
               CardViewHolder h = (CardViewHolder) holder;
               h.onBind(data, position);
           }
    }


    @Override
    protected CardStackView.ViewHolder onCreateView(ViewGroup parent, int viewType) {



        View view = getLayoutInflater().inflate(R.layout.list_card_item_larger_header, parent, false);
        return new CardViewHolder(view);
    }

    static class CardViewHolder extends CardStackView.ViewHolder {
        View mLayout;
        View mContainerContent;
        TextView mTextTitle;

        public CardViewHolder(View view) {
            super(view);
            mLayout = view.findViewById(R.id.frame_list_card_item);
            mContainerContent = view.findViewById(R.id.container_list_content);
            mTextTitle = (TextView) view.findViewById(R.id.text_list_card_title);
        }

        public void onBind(Integer data, int position) {
            mLayout.getBackground().setColorFilter(ContextCompat.getColor(getContext(), data), PorterDuff.Mode.SRC_IN);
            mTextTitle.setText(String.valueOf(position));

            itemView.findViewById(R.id.text_view).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //调用这个方法是，切换item 的形态，
                   // ((CardStackView) itemView.getParent()).performItemClick(CardViewHolder.this);
                }
            });
        }        @Override
        public void onItemExpand(boolean b) {
            mContainerContent.setVisibility(b ? View.VISIBLE : View.GONE);
        }

        @Override
        protected void onAnimationStateChange(int state, boolean willBeSelect) {
            super.onAnimationStateChange(state, willBeSelect);
            if (state == CardStackView.ANIMATION_STATE_START && willBeSelect) {
                onItemExpand(true);
            }
            if (state == CardStackView.ANIMATION_STATE_END && !willBeSelect) {
                onItemExpand(false);
            }
        }



    }






}
