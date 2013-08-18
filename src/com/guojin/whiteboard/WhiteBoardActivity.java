package com.guojin.whiteboard;

import android.app.Activity;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.guojin.entities.BoardEntity;

public class WhiteBoardActivity extends Activity {
	
	private float oldDist = Float.NaN;	// 两触点之间的旧距离值
	private PointF oldMidPoint;			// 旧两触点中心点
	
	private boolean isTwoPointTouch = false;
	private long firstTouchTime = -1;
	
	private BoardEntity boardEntity = null;	// Board实体
	
	private BoardView boardView;	// Board View
	
	private RelativeLayout topbarLayout;
	private RadioGroup modeSelectGroup;		// 模式单选组
	private RadioGroup handDrawModeConfGroup;		// 手绘模式配置单选组
	
	private LinearLayout noteModeConfLayout;		// 便签模式总配置Layout
	private Button noteAddNewBtn;
	private ToggleButton noteTextSizeBtn;	// 便签字体大小设置按钮
	private ToggleButton noteStyleBtn;		// 便签样式设置按钮
	
	private RadioGroup noteStyleConfGroup;		// 便签样式配置单选组
	
	private LinearLayout noteTextSizeConfLayout;	// 便签字体大小配置Layout
	private TextView noteTextSizeTxt;		// 便签字体大小显示
	private SeekBar noteTextSizeSeekbar;	// 便签字体调整
	
	private TextView scaleTextView;		// 缩放级别显示
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		boardEntity = new BoardEntity(this);
		boardView = new BoardView(this, boardEntity);
		// 设置view可以获取焦点
		boardView.setFocusable(true);
		boardView.setFocusableInTouchMode(true);
		boardEntity.bindView(boardView);
		
		// 向布局中添加boardview
		FrameLayout layout = (FrameLayout)getLayoutInflater().inflate(R.layout.activity_whiteboard, null);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		boardView.setLayoutParams(lp);
		layout.addView(boardView, 0);
		
		setContentView(layout);
		
		// 初始化控件
		topbarLayout = (RelativeLayout)findViewById(R.id.topbar_layout);
		topbarLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});
		
		// 模式选择
		modeSelectGroup = (RadioGroup)findViewById(R.id.modesel_group);
		handDrawModeConfGroup = (RadioGroup)findViewById(R.id.handdraw_conf_bar);
		
		// 便签模式设置
		noteModeConfLayout = (LinearLayout)findViewById(R.id.note_conf_layout);
		noteModeConfLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});
		noteTextSizeBtn = (ToggleButton)findViewById(R.id.note_conf_textsize_btn);
		noteStyleBtn = (ToggleButton)findViewById(R.id.note_conf_style_btn);
		// 添加新便签按钮
		noteAddNewBtn = (Button)findViewById(R.id.note_add_new);
		noteAddNewBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boardEntity.addEntity();
			}
		});
		noteStyleConfGroup = (RadioGroup)findViewById(R.id.note_style_group);
		noteStyleConfGroup.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});
		noteTextSizeConfLayout = (LinearLayout)findViewById(R.id.note_textsize_layout);
		noteTextSizeConfLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});
		noteTextSizeTxt = (TextView)findViewById(R.id.note_textsize_txt);
		noteTextSizeSeekbar = (SeekBar)findViewById(R.id.note_textsize_sbar);
		
		// 缩放比例显示
		scaleTextView = (TextView)findViewById(R.id.scale_ratio_txt);
		scaleTextView.setText((int)(boardEntity.getTotalScale() * 100) + "%");
		
		// 模式选择监听器
		modeSelectGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (group.getCheckedRadioButtonId()) {
				case R.id.mode_handdraw_btn:
					// 手绘模式
					handDrawModeConfGroup.setVisibility(View.VISIBLE);
					noteModeConfLayout.setVisibility(View.GONE);
					noteStyleConfGroup.setVisibility(View.GONE);
					noteTextSizeConfLayout.setVisibility(View.GONE);
					
					// 修改模式
					boardEntity.changeMode(BoardEntity.MODE_HANDDRAW);
					break;
				case R.id.mode_picdraw_btn:
					// 图片模式
					handDrawModeConfGroup.setVisibility(View.GONE);
					noteModeConfLayout.setVisibility(View.GONE);
					noteStyleConfGroup.setVisibility(View.GONE);
					noteTextSizeConfLayout.setVisibility(View.GONE);
					
					// 修改模式
					boardEntity.changeMode(BoardEntity.MODE_PIC);
					break;
				case R.id.mode_notedraw_btn:
					// 便签模式
					handDrawModeConfGroup.setVisibility(View.GONE);
					noteModeConfLayout.setVisibility(View.VISIBLE);
					noteStyleConfGroup.setVisibility(View.GONE);
					noteTextSizeConfLayout.setVisibility(View.GONE);
					noteStyleBtn.setChecked(false);
					noteTextSizeBtn.setChecked(false);
					
					// 修改模式
					boardEntity.changeMode(BoardEntity.MODE_NOTE);
					break;
				}
			}
		});
		((RadioButton)modeSelectGroup.findViewById(R.id.mode_handdraw_btn)).setChecked(true);
		
		// 便签字体设置按钮监听
		noteTextSizeBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					noteStyleBtn.setChecked(false);
					noteTextSizeConfLayout.setVisibility(View.VISIBLE);
				} else {
					noteTextSizeConfLayout.setVisibility(View.GONE);
				}
			}
		});
		
		// 便签样式设置按钮监听
		noteStyleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					noteTextSizeBtn.setChecked(false);
					noteStyleConfGroup.setVisibility(View.VISIBLE);
				} else {
					noteStyleConfGroup.setVisibility(View.GONE);
				}
			}
		});
		
		// 便签样式选择监听器
		noteStyleConfGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.note_style_blue:
					boardEntity.setNoteStyleColor(getResources().getColor(R.color.note_style_blue));
					break;
				case R.id.note_style_green:
					boardEntity.setNoteStyleColor(getResources().getColor(R.color.note_style_green));
					break;
				case R.id.note_style_orange:
					boardEntity.setNoteStyleColor(getResources().getColor(R.color.note_style_orange));
					break;
				case R.id.note_style_purple:
					boardEntity.setNoteStyleColor(getResources().getColor(R.color.note_style_purple));
					break;
				case R.id.note_style_red:
					boardEntity.setNoteStyleColor(getResources().getColor(R.color.note_style_red));
					break;
				case R.id.note_style_gray:
					boardEntity.setNoteStyleColor(getResources().getColor(R.color.note_style_gray));
					break;
				}
			}
		});
	}
	
	/**
	 * 获取两触点之间的距离
	 * @param event
	 * @return
	 */
	private float getPointsDist(MotionEvent event) {
		float dx = event.getX(0) - event.getX(1);
		float dy = event.getY(0) - event.getY(1);
		return (float)Math.sqrt(dx * dx + dy * dy);
	}
	
	/**
	 * 获取两触点之间的中心点
	 * @param event
	 * @return
	 */
	private PointF getMidPoint(MotionEvent event) {
		float x1 = event.getX(0);
		float y1 = event.getY(0);
		float x2 = event.getX(1);
		float y2 = event.getY(1);
		
		int[] loc = new int[2];
		boardView.getLocationOnScreen(loc);
		
		return new PointF((x1 + x2) / 2, (y1 + y2) / 2 - loc[1]);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		// 触点数目
		int pointerCount = event.getPointerCount();
		
		// 缩放比例
		float scale = 1;
		// 偏移距离
		float dx = 0f;
		float dy = 0f;
		
//		Log.d("DevLog", String.format("touch position: %f,%f", event.getX(), event.getY()));
		
		if (pointerCount == 2) {
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_MOVE:
				
				// 计算缩放比例scale
				if (Float.isNaN(oldDist)) {
					oldDist = getPointsDist(event);
				} else {
					float newDist = getPointsDist(event);
					
					if (Math.abs(newDist - oldDist) > 1f) {
						scale = newDist / oldDist;
						oldDist = newDist;
					} 
				}
				
				// 计算偏移距离
				if (oldMidPoint == null) {
					oldMidPoint = getMidPoint(event);
				} else {
					PointF newMidPoint = getMidPoint(event);
					
					float tdx = newMidPoint.x - oldMidPoint.x;
					float tdy = newMidPoint.y - oldMidPoint.y;
					
					if (Math.abs(tdx) > 1f || Math.abs(tdy) > 1f) {
						dx = tdx;
						dy = tdy;
						oldMidPoint = newMidPoint;
					}
				}
				
				PointF currMidPoint = getMidPoint(event);
				boardEntity.calculate(currMidPoint.x, currMidPoint.y, scale
						, dx, dy, boardView.getWidth(), boardView.getHeight());
				// 显示缩放比例
				scaleTextView.setText((int)(boardEntity.getTotalScale() * 100) + "%");
				boardView.postInvalidate();
				
				
//				Log.d("DevLog", String.format("Scale: %f\nDist: %f , %f", scale, dx, dy));
				break;
			case MotionEvent.ACTION_POINTER_UP:
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				oldDist = getPointsDist(event);
				oldMidPoint = getMidPoint(event);
				break;
			}
		} else {
			boardEntity.onEntityTouchEvent(event);
		}
		
		return false;
	}
}
