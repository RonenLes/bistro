package entities;

import java.time.LocalDate;

public class Employee {

	private String employeeID;
	private String role;
	private LocalDate hireDate;
	
	public Employee() {
		
	}
	
	
	public Employee(String employeeID, String role, LocalDate hireDate) {
		super();
		this.employeeID = employeeID;
		this.role = role;
		this.hireDate = hireDate;
	}



	public String getEmployeeID() {
		return employeeID;
	}

	public void setEmployeeID(String employeeID) {
		this.employeeID = employeeID;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public LocalDate getHireDate() {
		return hireDate;
	}

	public void setHireDate(LocalDate hireDate) {
		this.hireDate = hireDate;
	}
	
}
