package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(exclude = {"warning"})
@Entity
public class Token implements Serializable {
    @Id
    private String content;
    private boolean consumed;

    @OneToOne(mappedBy = "token")
    private Warning warning;

    public Token(String content, Warning warning) {
        this.content = content;
        this.warning = warning;
        this.consumed = false;
    }
}
