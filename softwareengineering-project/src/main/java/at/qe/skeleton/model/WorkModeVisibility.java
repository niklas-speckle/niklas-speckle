package at.qe.skeleton.model;

import lombok.Getter;

/**
 * Enumeration that states, who can see Userx's work mode:
 * public: everyone can see it
 * private: only members of the same group can see it
 * hidden: no one can see it
 */
@Getter
public enum WorkModeVisibility {
    PUBLIC ("Public"),
    PRIVATE ("Private"),
    HIDDEN ("Hidden");

    private final String name;

    WorkModeVisibility(String name) {
        this.name = name;
    }

    @Override
    public String toString(){
        return name;
    }

}
