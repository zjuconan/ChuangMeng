drop table lesson_faq;
drop table user_task;
drop table lesson_task;
drop table lesson_note;
drop table lesson_comment;
drop table course_comment;
drop table lesson_statistics;
drop table course_statistics;
drop table course_lesson;
drop table lesson;
drop table course;
drop table course_type;
drop table user_basic;
drop table user_type;



create table user_type (
	type_cd		varchar(10)	not null primary key,
	type_desc	varchar(50)	not null
);

create table user_basic (
	user_id		int			not null	auto_increment primary key,
	user_name	varchar(50)	not null,
	user_pwd	varchar(50)	not null,
	nicky_name	varchar(50)	not null,
	email		varchar(50)	not null,
	user_type_cd	varchar(10)	not null,
	user_icon	varchar(100),
	
	foreign key user_basic_fk1(user_type_cd) references user_type (type_cd)	
);

create table course_type (
	type_cd		varchar(20)	not null primary key,
	type_name	varchar(50)	not null
);

create table course (
	course_id	smallint	not null	auto_increment primary key,
	course_name	varchar(50)	not null,
	course_type_cd	varchar(20)	not null,
	lecturer_id	int			not null,
	course_pic	varchar(100),
	course_length	smallint	not null,
	price		decimal(6, 2),
	simple_desc	varchar(50)	not null,
	detail_desc	text		not null,
	create_time	datetime	not null,
	update_time	datetime	not null,
	update_user	varchar(50)	not null,
	
	foreign key course_fk1(course_type_cd) references course_type(type_cd),
	foreign key course_fk2(lecturer_id) references user_basic(user_id)
);

create table lesson (
	lesson_id	smallint	not null auto_increment	primary key,
	lesson_name	varchar(50)	not null,
	course_id	smallint	not null,
	lesson_pic	varchar(100),
	lesson_length	smallint	not null,
	simple_desc	varchar(50)	not null,
	detail_desc	text,
	create_time	datetime	not null,
	update_time	datetime	not null,
	update_user	varchar(50)	not null,
	
	foreign key lesson_fk1(course_id) references course(course_id)
);

create table course_lesson (
	course_id	smallint	not null,
	lesson_id	smallint	not null,
	lesson_order	tinyint	not null,
	update_time	datetime	not null,
	update_user	varchar(50)	not null,
	
	primary key (course_id, lesson_id),
	foreign key course_lesson_fk1(course_id) references course(course_id),
	foreign key course_lesson_fk2(lesson_id) references lesson(lesson_id)
);

create table course_statistics (
	course_id	smallint	not null primary key,
	play_num	smallint	not null,
	good_cmnt_num	smallint	not null,
	bad_cmnt_num	smallint	not null,
	update_time	datetime	not null,
	update_user	varchar(50)	not null,
	
	foreign key course_statistics_fk1(course_id) references course(course_id)
);

create table lesson_statistics (
	lesson_id	smallint	not null primary key,
	play_num	smallint	not null,
	good_cmnt_num	smallint	not null,
	bad_cmnt_num	smallint	not null,
	update_time	datetime	not null,
	update_user	varchar(50)	not null,
	
	foreign key lesson_statistics_fk1(lesson_id) references lesson(lesson_id)
);

create table course_comment (
	comment_id	bigint	not null	auto_increment	primary key,
	course_id	smallint	not null,
	user_id		int		not null,
	comment		text	not null,
	comment_time	datetime	not null,
	update_time	datetime	not null,
	update_user	varchar(50)	not null,
	
	foreign key course_comment_fk1(course_id) references course(course_id)
);

create table lesson_comment (
	comment_id	bigint	not null	auto_increment	primary key,
	lesson_id	smallint	not null,
	user_id		int		not null,
	comment		text	not null,
	comment_time	datetime	not null,
	update_time	datetime	not null,
	update_user	varchar(50)	not null,
	
	foreign key lesson_comment_fk1(lesson_id) references lesson(lesson_id)
);

create table lesson_note (
	note_id		bigint	not null	auto_increment	primary key,
	lesson_id	smallint	not null,
	user_id		int		not null,
	note		text	not null,
	note_time	datetime	not null,
	update_time	datetime	not null,
	update_user	varchar(50)	not null,
	
	foreign key lesson_note_fk1(lesson_id) references lesson(lesson_id)
);

create table lesson_task (
	task_id		bigint	not null	auto_increment	primary key,
	lesson_id	smallint	not null,
	user_id		int		not null,
	task_subject	varchar(50)	not null,
	task_desc	text	not null,
	comment_time	datetime	not null,
	update_time	datetime	not null,
	update_user	varchar(50)	not null,
	
	foreign key lesson_task_fk1(lesson_id) references lesson(lesson_id)
);

create table user_task (
	user_id		int		not null,
	task_id		bigint	not null,
	task_content	text	not null,
	complete_time	datetime	not null,
	task_feedback	text,
	feedback_advisor	int	not null,
	feedback_time	datetime,
	update_time	datetime	not null,
	update_user	varchar(50)	not null,
	
	primary key(user_id, task_id),
	foreign key user_task_fk1(user_id) references user_basic(user_id),
	foreign key user_task_fk2(task_id) references lesson_task(task_id),
	foreign key user_task_fk3(feedback_advisor) references user_basic(user_id)
);

create table lesson_faq (
	faq_id		int		not null auto_increment	primary key,
	lesson_id	smallint	not null,
	faq_question	varchar(255)	not null,
	faq_answer	text	not null,
	faq_time	datetime	not null,
	update_time	datetime	not null,
	update_user	varchar(50)	not null,
	
	foreign key lesson_faq_fk1(lesson_id)	references lesson(lesson_id)
);