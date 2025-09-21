package com.trever.backend.api.recent.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRecentView is a Querydsl query type for RecentView
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRecentView extends EntityPathBase<RecentView> {

    private static final long serialVersionUID = 1218999659L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRecentView recentView = new QRecentView("recentView");

    public final com.trever.backend.common.entity.QBaseTimeEntity _super = new com.trever.backend.common.entity.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.trever.backend.api.user.entity.QUser user;

    public final com.trever.backend.api.vehicle.entity.QVehicle vehicle;

    public QRecentView(String variable) {
        this(RecentView.class, forVariable(variable), INITS);
    }

    public QRecentView(Path<? extends RecentView> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRecentView(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRecentView(PathMetadata metadata, PathInits inits) {
        this(RecentView.class, metadata, inits);
    }

    public QRecentView(Class<? extends RecentView> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.trever.backend.api.user.entity.QUser(forProperty("user")) : null;
        this.vehicle = inits.isInitialized("vehicle") ? new com.trever.backend.api.vehicle.entity.QVehicle(forProperty("vehicle"), inits.get("vehicle")) : null;
    }

}

