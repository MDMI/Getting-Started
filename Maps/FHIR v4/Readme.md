#Notes on Map Usage#

##Data Type Instant##
In the FHIR standard, the data type Instant, a date-time variable, is specified as 
   YYYY-MM-DDThh:mm:ss.sss+zz:zz (see https://www.hl7.org/fhir/datatypes.html#instant)

This is a “normal” date-time including seconds and their fractions plus time zone. When a source message with this data type does not include the finer specificity of seconds and time zone, MDMI transformations do not append default values. E.g. “00:00:00Z” for seconds and time zone will not be appended. Therefore, this may result in validation errors.
