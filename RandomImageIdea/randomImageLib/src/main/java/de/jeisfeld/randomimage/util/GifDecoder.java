package de.jeisfeld.randomimage.util;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Decoder for animated gif.
 */
public class GifDecoder {
	/**
	 * The maximum stack size.
	 */
	private static final int MAX_STACK_SIZE = 4096;
	/**
	 * The minimum delay.
	 */
	public static final int MIN_DELAY = 100;
	/**
	 * The minimum delay enforce threshold.
	 */
	public static final int MIN_DELAY_ENFORCE_THRESHOLD = 20;

	/**
	 * The input stream for the GIF.
	 */
	private InputStream mIn;
	/**
	 * Full mImage mWidth.
	 */
	private int mWidth;
	/**
	 * Full mImage mHeight.
	 */
	private int mHeight;
	/**
	 * Global color table used.
	 */
	private boolean mGctFlag;
	/**
	 * Size of global color table.
	 */
	private int mGctSize;
	/**
	 * iterations; 0 = repeat forever.
	 */
	private int mLoopCount = 1;
	/**
	 * global color table.
	 */
	private int[] mGct;
	/**
	 * local color table.
	 */
	private int[] mLct;
	/**
	 * active color table.
	 */
	private int[] mAct;
	/**
	 * background color index.
	 */
	private int mBgIndex;
	/**
	 * background color.
	 */
	private int mBgColor;
	/**
	 * previous bg color.
	 */
	private int mLastBgColor;
	/**
	 * local color table flag.
	 */
	private boolean mLctFlag;
	/**
	 * mInterlace flag.
	 */
	private boolean mInterlace;
	/**
	 * local color table size.
	 */
	private int mLctSize;
	/**
	 * current image rectangle x.
	 */
	private int mIx;
	/**
	 * current image rectangle y.
	 */
	private int mIy;
	/**
	 * current image rectangle width.
	 */
	private int mIw;
	/**
	 * current image rectangle height.
	 */
	private int mIh;
	/**
	 * last image rectangle x.
	 */
	private int mLrx;
	/**
	 * last image rectangle y.
	 */
	private int mLry;
	/**
	 * last image rectangle width.
	 */
	private int mLrw;
	/**
	 * last image rectangle height.
	 */
	private int mLrh;
	/**
	 * current frame.
	 */
	private Bitmap mImage;
	/**
	 * previous frame.
	 */
	private Bitmap mLastBitmap;
	/**
	 * current data block.
	 */
	private byte[] mBlock = new byte[256]; // MAGIC_NUMBER
	/**
	 * Block size last graphic control extension info.
	 */
	private int mBlockSize = 0;
	/**
	 * 0=no action; 1=leave mIn place; 2=restore to bg; 3=restore to prev.
	 */
	private int mDispose = 0;
	/**
	 * Last dispose value.
	 */
	private int mLastDispose = 0;
	/**
	 * use transparent color.
	 */
	private boolean mTransparency = false;
	/**
	 * Delay mIn milliseconds.
	 */
	private int mDelay = 0;
	/**
	 * transparent color index.
	 */
	private int mTransIndex;
	/**
	 * LZW decoder working array for prefix.
	 */
	private short[] mPrefix;
	/**
	 * LZW decoder working array for suffix.
	 */
	private byte[] mSuffix;
	/**
	 * LZW decoder working array for pixelStack.
	 */
	private byte[] mPixelStack;
	/**
	 * LZW decoder working array for pixels.
	 */
	private byte[] mPixels;
	/**
	 * mFrames read from current file.
	 */
	private ArrayList<GifFrame> mFrames;
	/**
	 * number of mFrames.
	 */
	private int mFrameCount;
	/**
	 * flag indicating read complete.
	 */
	private boolean mReadComplete;

	/**
	 * Default constructor.
	 */
	public GifDecoder() {
		mReadComplete = false;
	}

	/**
	 * Gets display duration for specified frame.
	 *
	 * @param n int index of frame
	 * @return mDelay mIn milliseconds
	 */
	public int getDelay(final int n) {
		mDelay = -1;
		if ((n >= 0) && (n < mFrameCount)) {
			mDelay = mFrames.get(n).mDelay;
			// meets browser compatibility standards
			if (mDelay < MIN_DELAY_ENFORCE_THRESHOLD) {
				mDelay = MIN_DELAY;
			}
		}
		return mDelay;
	}

	/**
	 * Gets the number of mFrames read from file.
	 *
	 * @return frame count
	 */
	public int getFrameCount() {
		return mFrameCount;
	}

	/**
	 * Gets the first (or only) mImage read.
	 *
	 * @return BufferedBitmap containing first frame, or null if none.
	 */
	public Bitmap getBitmap() {
		return getFrame(0);
	}

	/**
	 * Gets the "Netscape" iteration count, if any. A count of 0 means repeat indefinitiely.
	 *
	 * @return iteration count if one was specified, else 1.
	 */
	public int getLoopCount() {
		return mLoopCount;
	}

	/**
	 * Creates new frame mImage from current data (and previous mFrames as specified by their disposition codes).
	 */
	private void setPixels() {
		// expose destination mImage's mPixels as int array
		int[] dest = new int[mWidth * mHeight];
		// fill mIn starting mImage contents based on last mImage's mDispose code
		if (mLastDispose > 0) {
			if (mLastDispose == 3) { // MAGIC_NUMBER
				// use mImage before last
				int n = mFrameCount - 2;
				if (n > 0) {
					mLastBitmap = getFrame(n - 1);
				}
				else {
					mLastBitmap = null;
				}
			}
			if (mLastBitmap != null) {
				mLastBitmap.getPixels(dest, 0, mWidth, 0, 0, mWidth, mHeight);
				// copy mPixels
				if (mLastDispose == 2) {
					// fill last mImage rect area with background color
					int c = 0;
					if (!mTransparency) {
						c = mLastBgColor;
					}
					for (int i = 0; i < mLrh; i++) {
						int n1 = (mLry + i) * mWidth + mLrx;
						int n2 = n1 + mLrw;
						for (int k = n1; k < n2; k++) {
							dest[k] = c;
						}
					}
				}
			}
		}
		// copy each source line to the appropriate place mIn the destination
		int pass = 1;
		int inc = 8; // MAGIC_NUMBER
		int iline = 0;
		for (int i = 0; i < mIh; i++) {
			int line = i;
			if (mInterlace) {
				if (iline >= mIh) {
					pass++;
					switch (pass) {
					case 2:
						iline = 4; // MAGIC_NUMBER
						break;
					case 3: // MAGIC_NUMBER
						iline = 2;
						inc = 4; // MAGIC_NUMBER
						break;
					case 4: // MAGIC_NUMBER
						iline = 1;
						inc = 2;
						break;
					default:
						break;
					}
				}
				line = iline;
				iline += inc;
			}
			line += mIy;
			if (line < mHeight) {
				int k = line * mWidth;
				int dx = k + mIx; // start of line mIn dest
				int dlim = dx + mIw; // end of dest line
				if ((k + mWidth) < dlim) {
					dlim = k + mWidth; // past dest edge
				}
				int sx = i * mIw; // start of line mIn source
				while (dx < dlim) {
					// map color and insert mIn destination
					int index = (mPixels[sx++]) & 0xff; // MAGIC_NUMBER
					int c = mAct[index];
					if (c != 0) {
						dest[dx] = c;
					}
					dx++;
				}
			}
		}
		mImage = Bitmap.createBitmap(dest, mWidth, mHeight, Config.ARGB_4444);
	}

	/**
	 * Gets the mImage contents of frame n.
	 *
	 * @param n the frame number.
	 * @return BufferedBitmap representation of frame, or null if n is invalid.
	 */
	public Bitmap getFrame(final int n) {
		if (mFrameCount <= 0) {
			return null;
		}
		return mFrames.get(n % mFrameCount).mImage;
	}

	/**
	 * Reads GIF image from stream.
	 *
	 * @param inputStream the input stream containing GIF file.
	 * @throws IOException error while processing
	 */
	public void read(final InputStream inputStream) throws IOException {
		init();
		if (inputStream != null) {
			mIn = inputStream;
			readHeader();
			readContents();
			if (mFrameCount < 0) {
				throw new IOException("Format Error");
			}
		}
		else {
			throw new IOException("Open error");
		}
		mReadComplete = true;
	}

	/**
	 * Decodes LZW mImage data into pixel array. Adapted from John Cristy's BitmapMagick.
	 *
	 * @throws IOException error while processing
	 */
	private void decodeBitmapData() throws IOException {
		int npix = mIw * mIh;
		if ((mPixels == null) || (mPixels.length < npix)) {
			mPixels = new byte[npix]; // allocate new pixel array
		}
		if (mPrefix == null) {
			mPrefix = new short[MAX_STACK_SIZE];
		}
		if (mSuffix == null) {
			mSuffix = new byte[MAX_STACK_SIZE];
		}
		if (mPixelStack == null) {
			mPixelStack = new byte[MAX_STACK_SIZE + 1];
		}
		// Initialize GIF data stream decoder.
		int dataSize = read();
		int clear = 1 << dataSize;
		int endOfInformation = clear + 1;
		int available = clear + 2;
		int nullCode = -1;
		int oldCode = nullCode;
		int codeSize = dataSize + 1;
		int codeMask = (1 << codeSize) - 1;
		int code;
		for (code = 0; code < clear; code++) {
			mPrefix[code] = 0; // XXX ArrayIndexOutOfBoundsException
			mSuffix[code] = (byte) code;
		}
		// Decode GIF pixel stream.
		int datum = 0;
		int bits = 0;
		int count = 0;
		int first = 0;
		int top = 0;
		int pi = 0;
		int bi = 0;

		int i = 0;
		while (i < npix) {
			if (top == 0) {
				if (bits < codeSize) {
					// Load bytes until there are enough bits for a code.
					if (count == 0) {
						// Read a new data mBlock.
						count = readBlock();
						if (count <= 0) {
							break;
						}
						bi = 0;
					}
					datum += ((mBlock[bi]) & 0xff) << bits; // MAGIC_NUMBER
					bits += 8; // MAGIC_NUMBER
					bi++;
					count--;
					continue;
				}
				// Get the next code.
				code = datum & codeMask;
				datum >>= codeSize;
				bits -= codeSize;
				// Interpret the code
				if ((code > available) || (code == endOfInformation)) {
					break;
				}
				if (code == clear) {
					// Reset decoder.
					codeSize = dataSize + 1;
					codeMask = (1 << codeSize) - 1;
					available = clear + 2;
					oldCode = nullCode;
					continue;
				}
				if (oldCode == nullCode) {
					mPixelStack[top++] = mSuffix[code];
					oldCode = code;
					first = code;
					continue;
				}
				final int inCode = code;
				if (code == available) {
					mPixelStack[top++] = (byte) first;
					code = oldCode;
				}
				while (code > clear) {
					mPixelStack[top++] = mSuffix[code];
					code = mPrefix[code];
				}
				first = (mSuffix[code]) & 0xff; // MAGIC_NUMBER
				// Add a new string to the string table,
				if (available >= MAX_STACK_SIZE) {
					break;
				}
				mPixelStack[top++] = (byte) first;
				mPrefix[available] = (short) oldCode;
				mSuffix[available] = (byte) first;
				available++;
				if (((available & codeMask) == 0) && (available < MAX_STACK_SIZE)) {
					codeSize++;
					codeMask += available;
				}
				oldCode = inCode;
			}
			// Pop a pixel off the pixel stack.
			top--;
			mPixels[pi++] = mPixelStack[top];
			i++;
		}
		for (i = pi; i < npix; i++) {
			mPixels[i] = 0; // clear missing mPixels
		}
	}

	/**
	 * Initializes or re-initializes reader.
	 */
	private void init() {
		mFrameCount = 0;
		mFrames = new ArrayList<>();
		mGct = null;
		mLct = null;
	}

	/**
	 * Reads a single byte from the input stream.
	 *
	 * @return the read byte.
	 * @throws IOException exception while reading.
	 */
	private int read() throws IOException {
		return mIn.read();
	}

	/**
	 * Reads next variable length mBlock from input.
	 *
	 * @return number of bytes stored mIn "buffer"
	 * @throws IOException error while processing
	 */
	private int readBlock() throws IOException {
		mBlockSize = read();
		int n = 0;
		if (mBlockSize > 0) {
			try {
				int count;
				while (n < mBlockSize) {
					count = mIn.read(mBlock, n, mBlockSize - n);
					if (count == -1) {
						break;
					}
					n += count;
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			if (n < mBlockSize) {
				throw new IOException("Format error");
			}
		}
		return n;
	}

	/**
	 * Reads color table as 256 RGB integer values.
	 *
	 * @param ncolors int number of colors to read
	 * @return int array containing 256 colors (packed ARGB with full alpha)
	 * @throws IOException error while processing
	 */
	private int[] readColorTable(final int ncolors) throws IOException {
		int nbytes = 3 * ncolors; // MAGIC_NUMBER
		int[] tab;
		byte[] c = new byte[nbytes];
		int n = 0;
		try {
			n = mIn.read(c);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		if (n < nbytes) {
			throw new IOException("Format error");
		}
		else {
			tab = new int[256]; // MAGIC_NUMBER max size to avoid bounds checks
			int i = 0;
			int j = 0;
			while (i < ncolors) {
				int r = (c[j++]) & 0xff; // MAGIC_NUMBER
				int g = (c[j++]) & 0xff; // MAGIC_NUMBER
				int b = (c[j++]) & 0xff; // MAGIC_NUMBER
				tab[i++] = 0xff000000 | (r << 16) | (g << 8) | b; // MAGIC_NUMBER
			}
		}
		return tab;
	}

	/**
	 * Main file parser. Reads GIF content blocks.
	 *
	 * @throws IOException error while processing
	 */
	public void readContents() throws IOException {
		// read GIF file content blocks
		boolean done = false;
		while (!done) {
			int code = read();
			switch (code) {
			case 0x2C: // MAGIC_NUMBER Image separator
				readBitmap();
				if (!mReadComplete) {
					return;
				}
				break;
			case 0x21: // MAGIC_NUMBER extension
				code = read();
				switch (code) {
				case 0xf9: // MAGIC_NUMBER graphics control extension
					readGraphicControlExt();
					break;
				case 0xff: // MAGIC_NUMBER application extension
					readBlock();
					StringBuilder app = new StringBuilder();
					for (int i = 0; i < 11; i++) { // MAGIC_NUMBER
						app.append((char) mBlock[i]);
					}
					if ("NETSCAPE2.0".equals(app.toString())) {
						readNetscapeExt();
					}
					else {
						skip(); // don't care
					}
					break;
				case 0xfe:// MAGIC_NUMBER comment extension
					skip();
					break;
				case 0x01:// plain text extension
					skip();
					break;
				default: // uninteresting extension
					skip();
				}
				break;
			case 0x3b: // MAGIC_NUMBER terminator
				done = true;
				break;
			case 0x00: // bad byte, but keep going and see what happens break;
			default:
				throw new IOException("Format error");
			}
		}
	}

	/**
	 * Reads Graphics Control Extension values.
	 *
	 * @throws IOException error while processing
	 */
	private void readGraphicControlExt() throws IOException {
		read(); // mBlock size
		int packed = read(); // packed fields
		mDispose = (packed & 0x1c) >> 2; // MAGIC_NUMBER disposal method
		if (mDispose == 0) {
			mDispose = 1; // elect to keep old mImage if discretionary
		}
		mTransparency = (packed & 1) != 0;
		mDelay = readShort() * 10; // MAGIC_NUMBER delay mIn milliseconds
		mTransIndex = read(); // transparent color index
		read(); // mBlock terminator
	}

	/**
	 * Reads GIF file header information.
	 *
	 * @throws IOException error while processing
	 */
	private void readHeader() throws IOException {
		StringBuilder id = new StringBuilder();
		for (int i = 0; i < 6; i++) { // MAGIC_NUMBER
			id.append((char) read());
		}
		if (!id.toString().startsWith("GIF")) {
			throw new IOException("Format error");
		}
		readLsd();
		if (mGctFlag) {
			mGct = readColorTable(mGctSize);
			mBgColor = mGct[mBgIndex];
		}
	}

	/**
	 * Reads next frame mImage.
	 *
	 * @throws IOException error while processing
	 */
	private void readBitmap() throws IOException {
		mIx = readShort(); // (sub)mImage position & size
		mIy = readShort();
		mIw = readShort();
		mIh = readShort();
		int packed = read();
		mLctFlag = (packed & 0x80) != 0; // MAGIC_NUMBER 1 - local color table flag mInterlace
		mLctSize = (int) Math.pow(2, (packed & 0x07) + 1); // MAGIC_NUMBER
		// 3 - sort flag
		// 4-5 - reserved mLctSize = 2 << (packed & 7); // 6-8 - local color
		// table size
		mInterlace = (packed & 0x40) != 0; // MAGIC_NUMBER
		if (mLctFlag) {
			mLct = readColorTable(mLctSize); // read table
			mAct = mLct; // make local table active
		}
		else {
			mAct = mGct; // make global table active
			if (mBgIndex == mTransIndex) {
				mBgColor = 0;
			}
		}
		int save = 0;
		if (mTransparency) {
			save = mAct[mTransIndex];
			mAct[mTransIndex] = 0; // set transparent color if specified
		}
		if (mAct == null) {
			throw new IOException("Format error"); // no color table defined
		}
		decodeBitmapData(); // decode pixel data
		skip();
		mFrameCount++;
		// create new mImage to receive frame data
		mImage = Bitmap.createBitmap(mWidth, mHeight, Config.ARGB_4444);
		setPixels(); // transfer pixel data to mImage
		mFrames.add(new GifFrame(mImage, mDelay)); // add mImage to frame
		// list
		if (mTransparency) {
			mAct[mTransIndex] = save;
		}
		resetFrame();
	}

	/**
	 * Reads Logical Screen Descriptor.
	 *
	 * @throws IOException error while processing
	 */
	private void readLsd() throws IOException {
		// logical screen size
		mWidth = readShort();
		mHeight = readShort();
		// packed fields
		int packed = read();
		mGctFlag = (packed & 0x80) != 0; // MAGIC_NUMBER 1 : global color table flag
		// 2-4 : color resolution
		// 5 : mGct sort flag
		mGctSize = 2 << (packed & 7); // MAGIC_NUMBER 6-8 : mGct size
		mBgIndex = read(); // background color index
		read(); // pixel aspect ratio
	}

	/**
	 * Reads Netscape extenstion to obtain iteration count.
	 *
	 * @throws IOException error while processing
	 */
	private void readNetscapeExt() throws IOException {
		do {
			readBlock();
			if (mBlock[0] == 1) {
				// loop count sub-mBlock
				int b1 = (mBlock[1]) & 0xff; // MAGIC_NUMBER
				int b2 = (mBlock[2]) & 0xff; // MAGIC_NUMBER
				mLoopCount = (b2 << 8) | b1; // MAGIC_NUMBER
			}
		}
		while (mBlockSize > 0);
	}

	/**
	 * Reads next 16-bit value, LSB first.
	 *
	 * @return the read value.
	 * @throws IOException error while processing
	 */
	private int readShort() throws IOException {
		// read 16-bit value, LSB first
		return read() | (read() << 8); // MAGIC_NUMBER
	}

	/**
	 * Resets frame state for reading next mImage.
	 */
	private void resetFrame() {
		mLastDispose = mDispose;
		mLrx = mIx;
		mLry = mIy;
		mLrw = mIw;
		mLrh = mIh;
		mLastBitmap = mImage;
		mLastBgColor = mBgColor;
		mDispose = 0;
		mTransparency = false;
		mDelay = 0;
		mLct = null;
	}

	/**
	 * Skips variable length blocks up to and including next zero length mBlock.
	 *
	 * @throws IOException error while processing
	 */
	private void skip() throws IOException {
		do {
			readBlock();
		}
		while (mBlockSize > 0);
	}

	/**
	 * A GIF frame.
	 */
	private static class GifFrame {
		/**
		 * The image.
		 */
		private Bitmap mImage;
		/**
		 * The delay.
		 */
		private int mDelay;

		/**
		 * Constructor.
		 *
		 * @param image The image.
		 * @param delay The delay.
		 */
		GifFrame(final Bitmap image, final int delay) {
			mImage = image;
			mDelay = delay;
		}
	}
}
