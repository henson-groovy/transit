package transit

import groovy.sql.Sql

class StudentNotFound extends Exception {
  String StudentID
  
  StudentNotFound(String studentID) {
    this.StudentID = studentID
  }
  
  String toString() {
    return "Student with ID '${this.StudentID}' does not exist."
  }
}

class Student {
  String StudentID
  String FirstName
  String LastName
  String PIDM

  Student(String studentID, String firstName, String lastName, Sql sql=null) {
    this.StudentID = studentID
    this.FirstName = firstName
    this.LastName = lastName
    
    if (sql) {
      String qSpriden = '''select spriden_pidm,
                                  spriden_first_name,
                                  spriden_last_name
                             from spriden
                            where spriden_id = ?
                              and spriden_change_ind is null'''
      def qResult = sql.firstRow(qSpriden, [studentID])
      if(!qResult) {
        throw new StudentNotFound(studentID)
      } else {
        this.PIDM = qResult.spriden_pidm
        this.FirstName = qResult.spriden_first_name
        this.LastName = qResult.spriden_last_name  
      }              
    }
  }
}
