package sernet.gs.reveng;

// Generated Jun 5, 2015 1:28:30 PM by Hibernate Tools 3.4.0.CR1

/**
 * SysHilfe generated by hbm2java
 */
public class SysHilfe implements java.io.Serializable {

	private String shlKontext;
	private int shlHlfId;

	public SysHilfe() {
	}

	public SysHilfe(String shlKontext, int shlHlfId) {
		this.shlKontext = shlKontext;
		this.shlHlfId = shlHlfId;
	}

	public String getShlKontext() {
		return this.shlKontext;
	}

	public void setShlKontext(String shlKontext) {
		this.shlKontext = shlKontext;
	}

	public int getShlHlfId() {
		return this.shlHlfId;
	}

	public void setShlHlfId(int shlHlfId) {
		this.shlHlfId = shlHlfId;
	}

}