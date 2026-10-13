package com.bank.repository.csv;

import com.bank.model.Employee;
import com.bank.model.EmployeeStatus;
import com.bank.model.Role;
import com.bank.repository.EmployeeRepository;
import com.bank.repository.InMemoryEmployeeRepository;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Stores the password hash, never the password. failedLogins is saved too, so a lockout survives a restart. */
public class CsvEmployeeRepository implements EmployeeRepository {

    private final CsvFile file;
    private final InMemoryEmployeeRepository memory = new InMemoryEmployeeRepository();

    public CsvEmployeeRepository(Path path) {
        this.file = new CsvFile(path,
                "employeeId", "fullName", "role", "status", "passwordHash",
                "passwordChangeRequired", "failedLogins");
        file.readAll(CsvEmployeeRepository::fromRow).forEach(memory::add);
    }

    @Override
    public void add(Employee employee) {
        memory.add(employee);
        save();
    }

    @Override
    public void update(Employee employee) {
        memory.update(employee);
        save();
    }

    private void save() {
        file.writeAll(memory.findAll(), CsvEmployeeRepository::toRow);
    }

    @Override
    public Optional<Employee> findById(String employeeId) {
        return memory.findById(employeeId);
    }

    @Override
    public List<Employee> findAll() {
        return memory.findAll();
    }

    @Override
    public String nextId() {
        return memory.nextId();
    }

    private static Map<String, Object> toRow(Employee employee) {
        Map<String, Object> row = new HashMap<>();
        row.put("employeeId", employee.getEmployeeId());
        row.put("fullName", employee.getFullName());
        row.put("role", employee.getRole());
        row.put("status", employee.getStatus());
        row.put("passwordHash", employee.getPasswordHash());
        row.put("passwordChangeRequired", employee.isPasswordChangeRequired());
        row.put("failedLogins", employee.getFailedLogins());
        return row;
    }

    private static Employee fromRow(CsvRow row) {
        return Employee.restore(
                row.text("employeeId"),
                row.text("fullName"),
                row.enumValue("role", Role.class),
                row.enumValue("status", EmployeeStatus.class),
                row.text("passwordHash"),
                row.bool("passwordChangeRequired"),
                row.integer("failedLogins"));
    }
}
