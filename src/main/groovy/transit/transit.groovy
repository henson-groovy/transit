package transit

import static groovy.io.FileType.*

class Transit {
  static main(args) {
    // Setup SQL Connection and Logging Directory
    Connection conn = new Connection()
    Log log = new Log(conn.IODir)
    
    // Loop through the transcripts, sort and verify
    File xmlDir = new File(conn.IODir)
    xmlDir.eachFileMatch(FILES, ~/.+\.(XML|xml)/, { xmlFile ->
      def xmlTranscript = new XmlSlurper().parse(xmlFile)
      ArrayList attendancePeriods = []
      
      try {
	Student student = new Student(
	  xmlTranscript.Student.Person.SchoolAssignedPersonID.text(),
	  xmlTranscript.Student.Person.FirstName.text(),
	  xmlTranscript.Student.Person.LastName.text(), conn.DB)
	
	log.setPrefix(student.StudentID)
	log.logIt("Beginning transcript processing.")
		  
	if(xmlTranscript.Student.AcademicRecord.AcademicSession.size() == 0) {
	  throw new EmptyAttendancePeriods()     
	}
	
    
	xmlTranscript.Student.AcademicRecord.AcademicSession.each { session ->
	  try {
	    AttendancePeriod attendancePeriod = new AttendancePeriod(
	      session.AcademicSesionDetail.SessionDesignator.text(),
	      xmlTranscript.TransmissionData.Source.Organization.text().padLeft(6, '0'), conn.DB)
	    
	    log.setPrefix("${student.StudentID} ${attendancePeriod.SBGICode} ${attendancePeriod.termCode}")
	    log.logIt("Setting up attendance period.")
	  
	    // Collect the courses under each section
     
	    session.Course.each {
	      try {
		Course course = new Course(
		  it.CourseCreditEarned.text(),
		  it.CourseAcademicGrade.text(),
		  it.CourseSubjectAbbreviation.text(),
		  it.CourseNumber.text(),
		  it.CourseTitle.text(),
		  attendancePeriod.TermCode, conn.DB)
			  
		if (course.doesntExist(student.PIDM, attendancePeriod.SBGICode, conn.DB)) {
		  attendancePeriod.Courses.add(course)
		  log.logIt("Adding course, '${course.Subject} ${course.Number}'.")
		}     
	      } catch(CreditsAreNaN | CourseAlreadyAdded e) {
		log.logIt(e.toString(), true)
	      } 
	    }
	  
	    if(attendancePeriod.Courses.size() > 0) {
	      attendancePeriods.add(attendancePeriod)
	    } else {
	      throw new EmptyAttendancePeriod()
	    }
	  } catch(EmptyAttendancePeriod | InvalidAttendancePeriod e) {
	    log.logIt(e.toString(), true)
	  }
	} 
	
	if(attendancePeriods.size() == 0) {
	  log.logIt("No courses to be added to student record.", true)
	}
	attendancePeriods.sort{ it.TermCode }
	
	/* Finally, let's shove all this junk into Banner! */
	for (attendancePeriod in attendancePeriods) {
	  attendancePeriod.setTrit(student.PIDM, conn.DB)
	  attendancePeriod.setTram(student.PIDM, conn.DB)
	  attendancePeriod.Courses.sort { it.Subject + ' ' + it.Number }
	  for (course in attendancePeriod.Courses) {
	    course.setTrcr(student.PIDM, attendancePeriod.TritNo, attendancePeriod.TramNo, conn.DB)
	    course.setCourse(student.PIDM, attendancePeriod.TritNo, attendancePeriod.TramNo, conn.DB)
	  }
	}
	 
	def newFileName = xmlFile.getName()
	File processed = new File(conn.IODir + '\\Processed')
	while (!xmlFile.renameTo(new File(processed, newFileName))) {
	  if (newFileName.lastIndexOf('-') == -1) {
	    newFileName = newFileName[0..(newFileName.lastIndexOf('.')-1)] + '-1.xml'
	  } else {
	    newFileName = newFileName[0..(newFileName.lastIndexOf('-')-1)] + '-' +
			  "${(newFileName[(newFileName.lastIndexOf('-')+1)..(newFileName.lastIndexOf('.')-1)].toInteger()+1)}.xml"
	  }
	}   
      } catch(StudentNotFound | EmptyAttendancePeriods e) {
	log.logIt(e.toString(), true)
      }
    })
    
    // Close open files, connections
    conn.closeDB()
    log.closeLog()
  }
}
