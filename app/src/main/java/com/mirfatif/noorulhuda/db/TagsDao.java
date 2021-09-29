package com.mirfatif.noorulhuda.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface TagsDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  long create(TagEntity entity);

  @Update
  void updateTag(TagEntity tag);

  @Query("SELECT * FROM TagEntity ORDER BY timeStamp * -1")
  List<TagEntity> getTags();

  @Query("SELECT * FROM TagEntity WHERE id = :id")
  TagEntity getTag(int id);

  @Query(
      "SELECT * FROM TagEntity WHERE title = :title AND `desc` IS :desc AND timeStamp = :timeStamp")
  TagEntity getTag(String title, String desc, long timeStamp);

  @Delete
  void remove(TagEntity entity);
}
