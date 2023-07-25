
function mapInstanceIdentifierToFHIR_Reference(source, target, properties, conversionrule) {

     
    var fhirResource = org.mdmi.core.engine.javascript.Utils.getSemanticProperyQualifier(conversionrule, 'fhirResource:');
    target.setValueSafely('reference.type', fhirResource);

    if (properties.get('REFERENCEURL') != null) {
        if (source.getValue('extension') != null) {
            target.setValueSafely('reference.value', properties.get('REFERENCEURL') + fhirResource +  source.getValue('extension'));
        }

        if (source.getValue('root') != null) {
            target.setValueSafely('reference.value', properties.get('REFERENCEURL') + fhirResource + source.getValue('root'));
        }
    } else {

        if (source.getValue('extension') != null &amp;&amp; source.getValue('root') != null) {
            target.setValueSafely('reference.value', properties.get('REFERENCEURL') + fhirResource +source.getValue('root') + '::' + source.getValue('extension'));
        } else {
            if (source.getValue('extension') != null) {
                target.setValueSafely('reference.value', properties.get('REFERENCEURL') + fhirResource +source.getValue('extension'));
            }

            if (source.getValue('root') != null) {
                target.setValueSafely('reference.value', properties.get('REFERENCEURL') + fhirResource +source.getValue('root'));
            }
        }



    }
}
