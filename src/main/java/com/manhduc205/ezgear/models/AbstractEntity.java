package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;



import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
/**
 * @MappedSuperclass
 * Chia sẻ thuộc tính: kế thừa các thuộc tính, phương thức, và ánh xạ từ lớp siêu lớp mà không cần định nghĩa lại.
 * Không tạo bảng riêng: Lớp được chú thích với @MappedSuperclass sẽ không tạo ra một bảng trong cơ sở dữ liệu.
 *                       Thay vào đó, các thuộc tính của nó sẽ được ánh xạ vào các bảng của các lớp con.
 * */
public abstract class AbstractEntity implements Serializable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @CreationTimestamp
    private LocalDateTime updatedAt;

}
