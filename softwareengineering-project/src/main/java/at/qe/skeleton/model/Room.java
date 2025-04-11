package at.qe.skeleton.model;

import at.qe.skeleton.configs.DefaultLimits;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = "limitsList")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Room extends Metadata implements Serializable, Persistable<String> {
    @Id
    @Column(length = 50)
    private String roomNumber;

    private String floor;



    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Limits> limitsList;

    @Override
    public String getId() {
        return roomNumber;
    }

    @Override
    public boolean isNew(){
        return (getCreateDate() == null);
    }

    public Room() {
        this.limitsList = new ArrayList<>(SensorType.values().length);

        for(SensorType sensorType : SensorType.values()){
            Limits limits = new Limits(DefaultLimits.getDefaultLimits(sensorType));
            this.limitsList.add(limits);
        }
    }

}
