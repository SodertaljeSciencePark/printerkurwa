package nti.te4.printerkurwa.Models;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "printers")
public class Printer {
    
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  
  private String name;
  private String ip;
  private String accessCode;
  private String modelType;
  private String serial;
  private boolean hasCamera;
}