package com.menny.android.anysoftkeyboard.keyboards;

import java.security.InvalidParameterException;
import java.util.HashMap;

import android.view.KeyEvent;

import com.menny.android.anysoftkeyboard.AnyKeyboardContextProvider;

class HardKeyboardSequenceHandler 
{
	private static final int[] msQwerty = new int[]{
		KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_T, KeyEvent.KEYCODE_Y,KeyEvent.KEYCODE_U,KeyEvent.KEYCODE_I,KeyEvent.KEYCODE_O,KeyEvent.KEYCODE_P,
		KeyEvent.KEYCODE_A,KeyEvent.KEYCODE_S,KeyEvent.KEYCODE_D,KeyEvent.KEYCODE_F,KeyEvent.KEYCODE_G,KeyEvent.KEYCODE_H,KeyEvent.KEYCODE_J,KeyEvent.KEYCODE_K,KeyEvent.KEYCODE_L,
		KeyEvent.KEYCODE_Z,KeyEvent.KEYCODE_X,KeyEvent.KEYCODE_C,KeyEvent.KEYCODE_V,KeyEvent.KEYCODE_B,KeyEvent.KEYCODE_N,KeyEvent.KEYCODE_M		
	};
	
	private abstract class KeyEventSequenceBase
	{
		protected KeyEventSequenceBase()
		{
		}
		
		public abstract int getSequenceLength();
		
		protected abstract int getIntAt(int i);
		
		@Override
		public boolean equals(Object o) 
		{
			if (o instanceof KeyEventSequenceBase)
			{
				KeyEventSequenceBase other = (KeyEventSequenceBase)o;
				if (other.hashCode() != hashCode())
					return false;
				if (other.getSequenceLength() != getSequenceLength())
					return false;
				for(int i=0;i<getSequenceLength();i++)
				{
					if (other.getIntAt(i) != getIntAt(i))
						return false;
				}
				//if I reached here... the it is equal.
				return true;
			}
			return super.equals(o);
		}
	}
	
	private class KeyEventSequence extends KeyEventSequenceBase
	{
		private final int[] mSequence;
		private final int mHashCode;
		private final char mTarget;
		
		public KeyEventSequence(int[] keyEventSequence, char target)
		{
			super();
			mTarget = target;
			mSequence = keyEventSequence;
			int hashCode = 0;
			for(int i=0;i<mSequence.length;i++)
				hashCode+=mSequence[i];
			
			mHashCode = hashCode;
		}
	
		public char getTarget() {return mTarget;}
		
		@Override
		public int getSequenceLength() {return mSequence.length;}
		
		@Override
		public int hashCode() {
			return mHashCode;
		}
		
		@Override
		protected int getIntAt(int i)
		{
			return mSequence[i];
		}
	}
	
	private class KeyEventSequenceHolder extends KeyEventSequenceBase
	{
		private final int[] mSequence;
		private int mHashCode;
		private int mCurrentSequenceLength;
		
		public KeyEventSequenceHolder()
		{
			super();
			mSequence = new int[10];//like there is going to be such a long sequence...
			mHashCode = 0;
			mCurrentSequenceLength = 0;
		}
		
		public void appendKeyEvent(int keyEvent)
		{
			mSequence[mCurrentSequenceLength % mSequence.length] = keyEvent;
			mCurrentSequenceLength++;
			mHashCode+=keyEvent;
		}
		
		public void reset()
		{
			mCurrentSequenceLength = 0;
			mHashCode = 0;
		}
		
		@Override
		public int getSequenceLength() {return mCurrentSequenceLength;}
		
		@Override
		public int hashCode() {
			return mHashCode;
		}
		
		@Override
		protected int getIntAt(int i)
		{
			return mSequence[i % mSequence.length];
		}
	}
	
	private final KeyEventSequenceHolder mCurrentTypedSequence;
	
	private final HashMap<KeyEventSequence, KeyEventSequence> mSequences;

	public HardKeyboardSequenceHandler()
	{
		mSequences = new HashMap<KeyEventSequence, KeyEventSequence>();
		mCurrentTypedSequence = new KeyEventSequenceHolder();
	}
	
	public void addQwertyTranslation(String targetCharacters)
	{
		if (msQwerty.length != targetCharacters.length())
			throw new InvalidParameterException("'targetCharacters' should be the same lenght as the latin QWERTY keys strings: "+msQwerty);
		for(int qwertyIndex=0; qwertyIndex<msQwerty.length; qwertyIndex++)
		{
			char latinCharacter = (char)msQwerty[qwertyIndex];
			char otherCharacter = targetCharacters.charAt(qwertyIndex);
			if (otherCharacter > 0)
				addSequence(new int[]{latinCharacter}, otherCharacter);
		}
	}
	
	public void addSequence(int[] sequence, char result)
	{
		//creating sub sequences
		for(int sequenceLength = 1; sequenceLength < sequence.length; sequenceLength++)
		{
			int[] subSequence = new int[sequenceLength];
			for(int i=0;i<sequenceLength;i++)
				subSequence[i] = sequence[i];
			
			KeyEventSequence keysSequence = new KeyEventSequence(subSequence, (char)0);
			
			if (!mSequences.containsKey(keysSequence))
				mSequences.put(keysSequence,keysSequence);
		}
		//add the real sequence mapping
		KeyEventSequence actualSequence = new KeyEventSequence(sequence, result);
		mSequences.put(actualSequence, actualSequence);
	}
	
	public char getSequenceCharacter(int currentKeyEvent, AnyKeyboardContextProvider inputHandler)
	{
		mCurrentTypedSequence.appendKeyEvent(currentKeyEvent);
		if (mSequences.containsKey(mCurrentTypedSequence))
		{
			KeyEventSequence mappedSequence = mSequences.get(mCurrentTypedSequence);
			char mappedChar = mappedSequence.getTarget();
			if (mappedChar == 0)
				return 0;
			else
			{
				//need to delete the already typed characters
				inputHandler.deleteLastCharactersFromInput(mappedSequence.getSequenceLength() - 1);
				return mappedChar;
			}
		}
		else
		{
			int lastSequenceLength = mCurrentTypedSequence.getSequenceLength();
			//the previous state is not valid
			mCurrentTypedSequence.reset();
			if (lastSequenceLength > 1)//the sequence is not there...
			{//maybe just this key event.
				return getSequenceCharacter(currentKeyEvent, inputHandler);
			}
			return 0; 
		}
	}
}