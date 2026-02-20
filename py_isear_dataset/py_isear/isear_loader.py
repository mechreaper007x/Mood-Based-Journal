import py_isear.enums as enums
import csv


class IsearSubset:

    def __init__(self,
                 labels,
                 values):
        self.labels = labels
        self.values = values


class IsearDataSet:

    def __init__(self,
                 data=IsearSubset([], []),
                 target=IsearSubset([], []),
                 text_data=[]):
        self.__data = data
        self.__target = target
        self.__text_data = text_data

    def get_data(self):
        return self.__data.values

    def get_target(self):
        return self.__target.values

    def get_data_label_at(self, i):
        return self.__data.labels[i]

    def get_target_label_at(self, i):
        return self.__target.labels[i]

    def get_freetext_content(self):
        return self.__text_data


class NoSuchFieldException:

    def __init__(self, field_name):
        self.message = "No such field in dataset : " + field_name

    def get_message(self):
        return self.message


class IsearLoader:

    def load_isear(self, s_isear_path):
        f_isear = open(s_isear_path, "r")
        '''
        The isear file extracted for the purpose of this initial
        loading is a pipe delimited csv-like file with headings
        '''

        isear_reader = csv.reader(f_isear,
                                  delimiter="|",
                                  quotechar='"')
        i = 0
        entry_attributes = []
        text_data = []
        entry_target = []
        for isear_row in isear_reader:
            if i == 0:
                i = i + 1
                continue
            result = self.__parse_entry(isear_row,
                                        i,
                                        text_data)
            entry_attributes.append(result["attributes"])
            entry_target.append(result["target"])
            i = i + 1
        attributes_subset = IsearSubset(self.attribute_list,
                                        entry_attributes)
        target_subset = IsearSubset(self.target_list,
                                    entry_target)
        return IsearDataSet(attributes_subset,
                            target_subset,
                            text_data)

    def __parse_entry(self,
                      isear_row,                        
                      index,              
                      text_data):                 
        i_col = 0
        l_attributes = []
        l_target = []
                                   
        for isear_col in isear_row:
                                                            
                                         
            if i_col >= len(enums.CONST_ISEAR_CODES):
                break

            s_cur_col = enums.CONST_ISEAR_CODES[i_col]

                                                                               
                                    
            b_is_sit = bool(s_cur_col == "SIT")
            if b_is_sit:
                if self.provide_text:
                                            
                    text_data.append(isear_col)
            else:
                                  

                if s_cur_col in self.attribute_list:
                    i_isear_col = int(isear_col)
                    l_attributes.append(i_isear_col)

                if s_cur_col in self.target_list:
                    i_isear_col = int(isear_col)
                    l_target.append(i_isear_col)
                         
            i_col = i_col + 1
                                                    
        return {"attributes": l_attributes,
                "target": l_target}

    def __init__(self,
                 attribute_list=[],
                 target_list=[],
                 provide_text=True):
                                                                 
        self.attribute_list = []
        self.set_attribute_list(attribute_list)
                                    
        self.target_list = []
        self.set_target_list(target_list)
                                           
        self.provide_text = provide_text

                                                      
    def __check_attr_exists(self, attribute):
        return attribute in enums.CONST_ISEAR_CODES

    def set_attribute_list(self, attrs):
        """Set a list of attributes to extract

        Args:
        attrs (list):  a list of strings refering Isear fields .

        Returns:
        self. in order to ease fluent programming (loader.set().set())
        Raises:
        NoSuchFieldException

        """
        self.attribute_list = []
        for attr in attrs:
            self.add_attribute(attr)
        return self

    def set_target_list(self, target):
        """Set a list of fields to extract as target
        Args:
        attrs (list):  a list of strings refering Isear fields .

        Returns:
        self. in order to ease fluent programming (loader.set().set())
        Raises:
        NoSuchFieldException

        """
        self.target_list = []
        for tgt in target:
            self.add_target(tgt)
        return self

    def set_provide_text(self, is_provide_text):
        """ Tell the extractor whether to load the free text field.
        Behaviour is true by default

        Args:
        is_provide_text (bool): whether to provide the text field or not
        Return
        self. For fluent API
        """
        self.provide_text = is_provide_text
        return self

    def add_attribute(self, attr):
        b_att_ex = self.__check_attr_exists(attr)
        if b_att_ex is not True:
            ex = NoSuchFieldException(attr)
            raise ex
        self.attribute_list.append(attr)
        return self

    def add_target(self, attr):
        b_att_ex = self.__check_attr_exists(attr)
        if b_att_ex is not True:
            ex = NoSuchFieldException(attr)
            raise ex
        self.target_list.append(attr)
        return self

                           
