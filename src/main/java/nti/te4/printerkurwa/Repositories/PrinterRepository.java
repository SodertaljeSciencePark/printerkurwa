package nti.te4.printerkurwa.Repositories;

import nti.te4.printerkurwa.Models.Printer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PrinterRepository extends JpaRepository<Printer, UUID> {
    boolean existsByIp(String ip);
    boolean existsByIpAndIdNot(String ip, UUID id);
}