package sernet.gs.reveng;

// Generated Jun 5, 2015 1:28:30 PM by Hibernate Tools 3.4.0.CR1

/**
 * FilterBasisId generated by hbm2java
 */
public class FilterBasisId implements java.io.Serializable {

	private String sucheId;
	private int fflId;
	private int fltId;

	public FilterBasisId() {
	}

	public FilterBasisId(String sucheId, int fflId, int fltId) {
		this.sucheId = sucheId;
		this.fflId = fflId;
		this.fltId = fltId;
	}

	public String getSucheId() {
		return this.sucheId;
	}

	public void setSucheId(String sucheId) {
		this.sucheId = sucheId;
	}

	public int getFflId() {
		return this.fflId;
	}

	public void setFflId(int fflId) {
		this.fflId = fflId;
	}

	public int getFltId() {
		return this.fltId;
	}

	public void setFltId(int fltId) {
		this.fltId = fltId;
	}

	public boolean equals(Object other) {
		if ((this == other))
			return true;
		if ((other == null))
			return false;
		if (!(other instanceof FilterBasisId))
			return false;
		FilterBasisId castOther = (FilterBasisId) other;

		return ((this.getSucheId() == castOther.getSucheId()) || (this
				.getSucheId() != null && castOther.getSucheId() != null && this
				.getSucheId().equals(castOther.getSucheId())))
				&& (this.getFflId() == castOther.getFflId())
				&& (this.getFltId() == castOther.getFltId());
	}

	public int hashCode() {
		int result = 17;

		result = 37 * result
				+ (getSucheId() == null ? 0 : this.getSucheId().hashCode());
		result = 37 * result + this.getFflId();
		result = 37 * result + this.getFltId();
		return result;
	}

}