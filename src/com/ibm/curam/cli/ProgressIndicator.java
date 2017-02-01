package com.ibm.curam.cli;

public class ProgressIndicator {

	private StringBuilder progress;
	private int total;
	
	/**
     * Progress indicator properties.
     */
    public ProgressIndicator(int total) {
        init(total);
    }

    public void updateProgress(int done) {
    	update(done, total);
    }
    
    public void resetTotal(int total) {
    	this.total = total;
    }
    
	/**
	 * called whenever the progress reporter needs to be updated. that is whenever
	 * progress was made.
	 *
	 * @param done an int representing the work done so far
	 * @param total an int representing the total work
	 */
	private void update(int done, int total) {
		char[] workchars = { '|', '/', '-', '\\' };
		String format = "\r%3d%% %s %c";

		int percent = (++done * 100) / total;
		int extrachars = (percent / 2) - this.progress.length();

		while (extrachars-- > 0) {
			progress.append('#');
		}

		System.out.printf(format, percent, progress, workchars[done % workchars.length]);

		if (done == total) {
			System.out.flush();
			System.out.println();
			init(total);
		}
	}

	private void init(int total) {
		this.progress = new StringBuilder(60);
		this.total = total;
	}
}
