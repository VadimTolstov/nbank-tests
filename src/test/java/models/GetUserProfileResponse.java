package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetUserProfileResponse extends BaseModel {
    private long id;
    private String username;
    private String name;
    private UserRole role;
}