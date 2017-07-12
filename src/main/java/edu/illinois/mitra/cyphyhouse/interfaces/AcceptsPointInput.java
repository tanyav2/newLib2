package edu.illinois.mitra.cyphyhouse.interfaces;

/**
 * Classes that implement this interface accept
 * user keystroke inputs into the GUI screen of the\
 * simulation
 */
public interface AcceptsPointInput
{
	void receivedPointInput(int x, int y);
}
