package com.guojin.entities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.util.ArrayList;

import android.R.array;
import android.content.ContentValues;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.Log;
import android.view.MotionEvent;

import com.guojin.store.DatabaseContract.PathDBEntity;

/**
 * 记录了一个path的相关信息,并且依据board的缩放对它进行缩放
 * 
 * @author jinux
 * 
 */
public class PathEntity extends Entity {

	long storeId = -1;

	@Override
	public long getID() {
		return storeId;
	}

	@Override
	public void setID(long id) {
		this.storeId = id;
	}

	private static final String TAG = "PathEntity";

	BoardEntity board;
	Matrix mMatrix;
	Paint mPaint;
	Path mPath;

	/**
	 * 录path被添加到board上时board的缩放比例--------------存储
	 */
	float originalScale;
	/**
	 * 当前board的比例,通过currentScale/orginalScale可求得path需要缩放的比例-- 存储
	 */
	float currentScale;
	/**
	 * path在board上的位置---------------------------------------- 存储
	 */
	double boardX, boardY;
	/**
	 * 笔触的大小------------------------------------------------------- 存储
	 */
	private int paintSize;
	/**
	 * 原始点的数组-----------------------------存储
	 */
	private ArrayList<float[]> pathPointsList;
	/**
	 * 笔触颜色-------------------------------------------------------------存储
	 */
	private int color;

	/**
	 * 相对屏幕左上角的坐标
	 */
	private float sx;
	private float sy;

	public PathEntity(BoardEntity b, int showIndex, Path path, int paintSize,
			int color, ArrayList<float[]> pointsList) {

		this.pathPointsList = pointsList;
		// 以点数组的第一个代表path的位置
		this.sx = pointsList.get(0)[0];
		this.sy = pointsList.get(0)[1];
		// 转化为board上的位置
		double xy[] = b.screenToBoardCoodTrans(this.sx, this.sy);
		this.boardX = (float) xy[0];
		this.boardY = (float) xy[1];

		initProperty(b, showIndex, (float) b.getTotalScale(),
				(float) b.getTotalScale(), path, paintSize, color);

	}

	private void initProperty(BoardEntity b, int showIndex,
			float originalScale, float curScale, Path path, int paintSize,
			int color) {
		this.board = b;
		this.showIndex = showIndex;
		this.originalScale = originalScale;
		this.currentScale = curScale;

		this.mMatrix = new Matrix();
		this.mPath = new Path(path);

		// 初始化paint
		this.mPaint = new Paint();
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setAntiAlias(true);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeJoin(Paint.Join.ROUND);

		this.color = color;
		this.paintSize = paintSize;
		mPaint.setColor(color);
		mPaint.setStrokeWidth((float) (paintSize * originalScale));
	}

	public PathEntity(BoardEntity b, long id, int showIndex,
			float originalScale, float currentScale, int color, int paintSize,
			double bx, double by, byte[] pointsBuf) {

		setID(id);

		// board和屏幕上的坐标
		this.boardX = bx;
		this.boardY = by;
		PointF p = b.boardToScreenCoodTrans(bx, by);
		this.sx = p.x;
		this.sy = p.y;

		// 构造path
		Path path = new Path();
		ArrayList<float[]> pointList = null;
		try {
			pointList = byteToPoints(pointsBuf);
		} catch (Exception e) {
			e.printStackTrace();
		}
		path.moveTo(pointList.get(0)[0], pointList.get(0)[1]);
		int i = 1;
		float[] pre = null;
		for (int size = pointList.size(); i < size - 1; i++) {
			pre = pointList.get(i - 1);
			path.quadTo(pre[0], pre[1], (pointList.get(i)[0] + pre[0]) / 2,
					(pointList.get(i)[1] + pre[1]) / 2);
		}
		path.lineTo(pointList.get(i)[0], pointList.get(i)[1]);

		initProperty(b, showIndex, originalScale, currentScale, path,
				paintSize, color);

		float originalSxy[] = pointList.get(0);
		Log.e(TAG, "sx=" + sx + ",sy=" + sy);
		Log.e(TAG, "ox=" + originalSxy[0] + ",oy=" + originalSxy[1]);
		transform(currentScale / originalScale, sx - originalSxy[0], sy
				- originalSxy[1]);
	}

	PointF tmpScreenPoint;
	float tmpScale;

	public void draw(Canvas canvas) {
		// 获取现在的缩放比例
		tmpScale = (float) board.getTotalScale();
		tmpScreenPoint = board.boardToScreenCoodTrans(boardX, boardY);

		transform((float) (tmpScale / currentScale), tmpScreenPoint.x - sx,
				tmpScreenPoint.y - sy);

		currentScale = tmpScale;
		this.sx = tmpScreenPoint.x;
		this.sy = tmpScreenPoint.y;

		mPaint.setStrokeWidth((float) (this.paintSize * currentScale));
		canvas.drawPath(mPath, mPaint);
	}

	private void transform(float scale, float dx, float dy) {
		Matrix mMatrix = new Matrix();
		// 如果缩放比例变动则对矩阵进行缩放处理
		mMatrix.setScale(scale, scale, this.sx, this.sy);
		// 如果位置发生变化,也对path进行平移变换
		mMatrix.postTranslate(dx, dy);
		mPath.transform(mMatrix);

	}

	@Override
	public void onEntityTouchEvent(MotionEvent event) {
		// 空函数

	}

	@Override
	public int getType() {
		return BoardEntity.TYPE_PATH_ENTITY;
	}

	@Override
	public boolean isInRange(float x, float y) {
		return false;
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private boolean containPoint(int x, int y) {
		RectF bounds = new RectF();
		mPath.computeBounds(bounds, true);
		Region region = new Region();
		region.setPath(mPath, new Region((int) bounds.left, (int) bounds.top,
				(int) bounds.right, (int) bounds.bottom));
		if (region.contains(x, y)) {
			return true;
		}
		return false;
	}

	@Override
	public void removeFocus() {
		// 空函数
	}

	static final float precision = 15f;

	/**
	 * 将point和path上的每一点进行距离计算,如果和某一点距离小于特定值,就表明该点在path上
	 * 
	 * @param point
	 * @return
	 */
	public boolean containPoint(PointF point) {
		ArrayList<PointF> list = getPointsArray(mPath, precision);
		Log.e(TAG, "list.size=" + list.size());
		for (PointF p : list) {
			float dist = distanceBetween(p, point);
			if (dist - precision < 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取path的一系列点
	 * 
	 * @return
	 */
	static ArrayList<PointF> getPointsArray(Path mPath, float precision) {
		ArrayList<PointF> list = new ArrayList<PointF>();
		PathMeasure pm = new PathMeasure(mPath, false);
		float[] coords = null;
		for (float i = 0 + precision; i <= pm.getLength(); i += precision) {
			coords = new float[2];
			pm.getPosTan(i, coords, null);
			list.add(new PointF(coords[0], coords[1]));
		}
		return list;
	}

	/**
	 * 计算两个点的距离
	 * 
	 * @param p1
	 * @param p2
	 * @return
	 */
	private float distanceBetween(PointF p1, PointF p2) {
		return (float) Math.sqrt(Math.pow(p1.x - p2.x, 2)
				+ Math.pow(p1.y - p2.y, 2));
	}

	@Override
	public ContentValues getContentValues() {
		ContentValues values = new ContentValues();
		values.put(PathDBEntity.BOARD_ID, board.getBoardID());
		values.put(PathDBEntity.SHOW_INDEX, showIndex + "");
		values.put(PathDBEntity.POS_X, boardX);
		values.put(PathDBEntity.POS_Y, boardY);

		values.put(PathDBEntity.CUR_SCALE, this.currentScale);
		values.put(PathDBEntity.ORI_SCALE, this.originalScale);
		values.put(PathDBEntity.STROKE_COLOR, this.color);
		values.put(PathDBEntity.STROKE_WIDTH, this.paintSize);
		values.put(PathDBEntity.POINTS, getByteFromPoints());
		return values;
	}

	private byte[] getByteFromPoints() {
		ByteArrayOutputStream obj = new ByteArrayOutputStream();
		try {
			ObjectOutputStream out = new ObjectOutputStream(obj);
			out.writeObject(this.pathPointsList);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return obj.toByteArray();
	}

	private ArrayList<float[]> byteToPoints(byte[] buf)
			throws OptionalDataException, ClassNotFoundException, IOException {
		ByteArrayInputStream obj = new ByteArrayInputStream(buf);
		ObjectInputStream in = new ObjectInputStream(obj);
		return (ArrayList<float[]>) in.readObject();
	}

}
